#!/software/bin/perl -w
#
# Deuce.pm
# 
# by Gary Williams
#
# Some methods for querying the WormBase datomic database.
#
# Last updated by: $Author: gw3 $     
# Last updated on: $Date: 2014-12-05 09:51:03 $      

=pod

=head1 NAME

 Deuce

=head1 SYNOPSIS

  use Dauth;
  use Deuce;
  my $dauth = Dauth->new('geneace');
  my $deuce = Deuce->new($dauth);

  my @ID_names = $deuce->fetch($class, $class_id_pattern);
  my @ID_names = $deuce->fetch($class, $class_id_pattern, $CRITERIA);

e.g.
  my @ID_names = $deuce->fetch('CDS', '*', "Remark#evidence/Curator_confirmed = 'WBPerson4025'; FOLLOW Gene"); # search for CDSs curated by WBPerson4025 then return the list of attached Genes

 my @ID_names = $deuce->fetch('CDS', '*', "COUNT Source_exons > 2 ; Isoform") # find CDSs with more than 2 exons and which are Isoforms

 my @ID_names = $deuce->fetch('Keyset', '~/Project/myGenes.set', "Live ; Corresponding_CDS"); # get the list of Genes from the keyset file that are Live and are coding.



The 'CRITERIA' specifier has the format:

CRITERIA = CRITERION [; CRITERION]                       # the Criteria are implicitly 'AND'ed together
CRITERION = IS_CRITERION | FOLLOW_CRITERION | COUNT_CRITERION | EXISTS_TAG_CRITERION | MISSING_TAG_CRITERION

IS_CRITERION = 'IS' pattern                              # find object whose name matches the pattern
pattern = string | wildcard_string

FOLLOW_CRITERION = 'FOLLOW' TAG_INDICATOR                # follow the tag to the new class and return its matching IDs instead of the original class
TAG_INDICATOR = name_of_ACE_tag | name_of_datomic_tag | name#hash_name/field
hash_name = name of hash class (e.g. 'evidence')
name_of_ACE_tag = ACE_TAG_name | ACE_TAG_name 'NEXT' | ACE_TAG_name^additional_name  # the additional name was added by Thomas to avoid any anonymous fields

COUNT_CRITERION = 'COUNT' TAG_INDICATOR OP number             # the number of existing TAGs must match the specified number

EXISTS_TAG_CRITERION = TAG_INDICATOR | TAG_INDICATOR OP VALUE      # the specified tag or tag with values must exist
OP = GT | > | LT | < | EQ | = | LE | <= | GE | >= | NE | NOT | !=
VALUE = string | wildcard_string | number

MISSING_TAG_CRITERION = 'NOT' EXISTS_TAG_CRITERION       # the specified tag or tag with value must not exist



=head1 DESCRIPTION

Methods for doing various searches in the datomic WormBase databases

Apart from the ACE-like routines 'fetch()' etc.  all class names are
assumed to be datomic class names with the '/id' suffix removed
e.g. 'cds' and fetch() will be able to use this as well as the
ACE-like name, for backwards compatibility.

It is useful to see how to do transactions in:
https://github.com/WormBase/db/tree/master/geneace/scripts/{batch_newfeature.pl,id_resurrect.pl}


=head1 CONTACT

Gary gw3@ebi.ac.uk

=head1 APPENDIX

 The rest of the documentation details each of the object methods.
 Internal methods are preceded with a _

=cut

package Deuce;

use Dauth;
use warnings;
use edn;          # Thomas Down's module for wrapping data in EDN


=head2

    Title   :   new
    Usage   :   my $deuce = Deuce->new($dauth_db);
    Function:   initialises the connection to the datomic REST server
    Returns :   Deuce object;
    Args    :   Dauth database connection

=cut

sub new {
  my $class = shift;
  my $self = {};
  bless $self, $class;

  my $db = shift;
  $self->{db} = $db;

  return $self;
  
}


#############################################################################
# Stuff for reading various bits of the schema
#############################################################################

=head2 

    Title   :   get_classes
    Usage   :   my $classes = $deuce->get_classes();
    Function:   returns the complete hash-ref of classes keyed on the datomic name of the class
            :   useful data includes:
            :     'db/ident' => name of the class in the datomic database (e.g. 'transposon/id')
            :     'pace/is-hash' => boolean, true if it is a hash (e.g. ?Evidence)
            :   creates the hash %ace_classes keyed on datomic class names (with and without the '/id') to give ACE class names
            :   creates the hash %datomic_classes keyed on ACE class names to give datomic class names (without the '/id')
    Returns :   the complete classes - hash keyed by datomic name of class (with and without the '/id')
    Args    :   

=cut

sub get_classes {
 my ($self) = @_;

 if (!defined $self->{classes}) {

   my $schema = ($self->{db})->get_schema();
   my %classes;
   my %ace_classes;
   my %datomic_classes;
   foreach my $class (@{$schema->{classes}}) {
     my $datomic_class_name = ${$class->{'db/ident'}};
     my $ace_class_name = $class->{'pace/identifies-class'};
     $classes{$datomic_class_name} = $class;
     $ace_classes{$datomic_class_name} = $ace_class_name; 
     $datomic_class_name =~ s/\/id//;
     $classes{$datomic_class_name} = $class;
     $ace_classes{$datomic_class_name} = $ace_class_name; 
     $datomic_classes{$ace_class_name} = $datomic_class_name; # without the '/id'
   }
   $self->{classes} = \%classes;
   $self->{ace_classes} = \%ace_classes;
   $self->{datomic_classes} = \%datomic_classes;
 }

 return $self->{classes};
}


#############################################################################

=head2 

    Title   :   get_attributes
    Usage   :   my $attributes = $deuce->get_attributes();
    Function:   returns the complete attributes of the classes
    Returns :   the complete attributes of the classes - array-ref of hashes
    Args    :   

=cut

sub get_attributes {
 my ($self) = @_;

 if (!defined $self->{attributes}) {

   my $schema = ($self->{db})->get_schema();
  
   my $attributes = $schema->{attributes};
   $self->{attributes} = $attributes
 }

 return $self->{attributes};
}


#############################################################################

=head2 

    Title   :   get_class_attributes
    Usage   :   my $class_desc = $deuce->get_class_attributes($class);
    Function:   returns an array holding the attributes for this class
    Returns :   array of hash holding the attributes of the class
    Args    :   class - string - ACE name of the class

=cut

sub get_class_attributes {
 my ($self, $class) = @_;

  if (!defined $self->{class_desc}{$class}) {
    my @desc;
    my $attributes = $self->get_attributes();

    my $class_name_in_datomic = $self->get_class_name_in_datomic($class);

    # look for this classes' attributes
    foreach my $attr (@{$attributes}) {
      my $ident = ${$attr->{'db/ident'}};
      if ($ident =~ /^${class_name_in_datomic}[\/\.]/i) {
	push @desc, $attr;
      }
    }

    # sort them by 'db/id' event number - this has the effect of
    # making the tags appear in the same consistent order every time,
    # even though the order may not be optimal.
    my @desc_sorted = sort {$a->{'db/id'} <=> $b->{'db/id'}} @desc;

    $self->{class_desc}{$class} = \@desc_sorted;
  }
  return $self->{class_desc}{$class};
}

#############################################################################

=head2 

    Title   :   get_class_name_in_datomic
    Usage   :   my $class_desc = $deuce->get_class_name_in_datomic($class);
    Function:   input an ACE class name and it returns the datomic equivalent of the ACE class name
    Returns :   returns the datomic equivalent of the ACE class name (i.e. with the '/id' suffix)
    Args    :   class - string - ACE class name or datomic class name

=cut

sub get_class_name_in_datomic {
  my ($self, $class) = @_;
  
  my $classes = $self->get_classes();
  if (!exists  $self->{datomic_classes}{$class}) {
    # warn "get_class_name_in_datomic(): Can't find datomic name of '$class', so we assume we already have a datomic name\n";
    return $class;
  }
  return $self->{datomic_classes}{$class};
}


#############################################################################

=head2 

    Title   :   get_class_name_in_ace
    Usage   :   my $class_desc = $deuce->get_class_name_in_ace($class);
    Function:   input an datomic class name and it returns the ACE equivalent of the datomic class name
    Returns :   returns the ACE equivalent of the datomic class name
    Args    :   class - string - class datomic name

=cut

sub get_class_name_in_ace {
  my ($self, $class) = @_;
  
  my $classes = $self->get_classes(); # ensure the ace_classes hash is populated
  if (!exists  $self->{ace_classes}{$class}) {
    warn "get_class_name_in_ace(): Can't find Ace name of $class, assuming we already have an Ace name\n";
    return $class;
  }
  return $self->{ace_classes}{$class};
}

#############################################################################

=head2 

    Title   :   is_class_a_hash
    Usage   :   my $boolean = $deuce->is_class_a_hash($class);
    Function:   returns whether the class is a hash class, like ?Evidence or not
    Returns :   boolean
    Args    :   class - string - class datomic name

=cut

sub is_class_a_hash {
  my ($self, $class) = @_;
  
  my $classes = $self->get_classes();
  return $classes->{$class}{'pace/is-hash'};
}

#############################################################################
# Stuff for getting the model in an ACE-like structure
#############################################################################

=head2 

    Title   :   output_class_model_as_ace
    Usage   :   my $model = $deuce->output_class_model_as_ace($class);
    Function:   returns the class model as an text ACE file
    Returns :   text ACE file class format
    Args    :   class - string - ACE name of the class
                $display_alternative_names - boolean - if true display the names the Thomas created to annotate the fields with no name

Not sure about the ?Feature Merged_into 'feature.merged-into/feature' field is this now a simple object 'db.type/ref' 'pace/obj-ref' and Acquires_merge which has disappeared

NB Thomas says: "In Datomic land, "in-bound" attributes always have an effective :db.cardinality/many."

Hash data at the end of the ACE line is always implicity UNIQUE (cardinality/one)

=cut

sub output_class_model_as_ace {
  my ($self, $class, $display_alternative_names) = @_;


  my $text; # holds the line being built
  my $model;

  # walk along the tag structure
  my $objstr = $self->model($class);

#  my $text = $self->display_class_text($objstr);

  my $element = 0;
  my $level=0;
  my @level; # element node last used at this level;
  my @indent; # indentation used at this level
  my @unique; # uniqueness indication at this level, TRUE if the previous level item points to this level with cardinality/one
  my $right;
  my $down;
  my $hash; # set of hashes from a component to add to the end of the line


  $level[$level] = $element;


  $text =  "?$class ";
  $indent[$level] = length $text;

  do {
    do {
      $text .= " " x ($indent[$level] - length($text));
      my $type;
      do {
	$level[$level] = $element;
	my $name = $objstr->[$element]->{name};
	my $alternative_field_name = $objstr->[$element]->{alternative_field_name}; # this flag is TRUE is $name is an alternative name
	$type = $objstr->[$element]->{type};
	if ($objstr->[$element]->{isUnique}) {$unique[$level] = 1} else {$unique[$level] = 0} # set up the UNIQUE status for the next level
	my $object = $objstr->[$element]->{xref}; # 'pace/obj-ref' object name like 'feature/id'
	my $component =  $objstr->[$element]->{isComponent};
	if ($component && defined $objstr->[$element]->{hash}) {$hash = $objstr->[$element]->{hash}}
	if (defined $object && $object ne '') {$object = $self->get_class_name_in_ace($object)}
	my $hidden = $objstr->[$element]->{hidden};
	my $remote_ace_tag = $objstr->[$element]->{remote_tag};
	if (!defined $remote_ace_tag) {$remote_ace_tag = ''} # some objects like 'locatable/id' (no class) and 'genetic-code/id' (no tags) don't have a tag to point back from.
	if (!$hidden) { # check if this node has things that we wish to display in the ACE display
	  if (defined $name && (!$alternative_field_name)) {$text .= "$name "}
	  if (defined $type) {
	    if ($type eq 'long') {$text .= "Int "} 
	    elsif ($type eq 'double') {$text .= "Float "} 
	    elsif ($type eq 'float') {$text .= "Float "} 
	    elsif ($type eq 'string') {$text .= "Text "}
	    elsif ($type eq 'instant') {$text .= "DateType "}
	    elsif ($type eq 'ref' && $object ne '') {$text .= "?${object} "} # ACE name of object - NB the nodes that are not components with a ref to enums don't have object names
	    elsif ($type eq 'incoming-ref' && $object ne '') {$text .= "?${object} INXREF $remote_ace_tag "}
	    elsif ($type eq 'outgoing-ref' && $object ne '') {$text .= "?${object} OUTXREF $remote_ace_tag "}
	    elsif ($type eq 'enum') {$text .= ""}
	  }
	  if ($alternative_field_name && $display_alternative_names) {$text .= "^$name "}
	}

	$right = $objstr->[$element]->{right};
	if (defined $right) {
	  if ($unique[$level] && !$objstr->[$right]->{hidden}) {$text .= "UNIQUE "} 
	  $element = $right;
	  $level++;
	  $type = $objstr->[$element]->{type}; # get type of next item on the line
	  if (defined $type && $type eq 'enum')  {$text .= "ENUM "}
	  $level[$level] = $right;
	  $indent[$level] = length $text;
#	  $text .= '> '; # debug
	}
      } while (defined $right);
      
      #############
      # end of line
      #############
      # is there a set of hash objects to append?
      my $line_len = length $text; 
      my $count = 0;
      foreach my $hsh (@{$hash}) {
	if ($count > 0) {$text .= " " x $line_len} # indent the next line
	my $h = $self->get_class_name_in_ace($hsh);
	$text .= "#${h}\n";
	$count++;
      }
      # tidy up the line and reset line variables ready for the next time round
      if (scalar @{$hash} == 0) {$text .= "\n"}
      $model .= $text;
      $text = "";

      $down = $objstr->[$element]->{down};
      if (defined $down) {
	$element = $down;
	$level[$level] = $down;
      }
      
    } while (defined $down);

    # no more nodes in this tree, go back some levels and down
    my $backtracking = 1;
    while ($backtracking) {
      $hash = undef;
      $level--;
      $element = $level[$level];
      $down = $objstr->[$element]->{down};
      if (defined $down) {
	$element = $down;
	$level[$level] = $element;
      }
      if (defined $down || $level < 0) {$backtracking = 0}
    }
  } while ($level >= 0);

  # extra blank line at the end
  $text .= "\n";
  $model .= $text;

  return $model;
}


#############################################################################

=head2 

    Title   :   model
    Usage   :   my $model = $deuce->model($class);
    Function:   returns the class model as an text ACE file
    Returns :   array-ref of class tags and values
    Args    :   string - datomic class name

=cut

sub model {
  my ($self, $class) = @_;

  my $class_name_in_datomic = $self->get_class_name_in_datomic($class);
  if (!defined $self->{class_object}{$class}) {

    my $class_attributes = $self->get_class_attributes($class_name_in_datomic); # attributes of class
    my $classes = $self->get_classes(); # hash of all classes' data
    my @class_desc_array = $classes->{$class_name_in_datomic};
    my %class_desc = %{$class_desc_array[0]};
    
    my @objstr = ();
    $self->parse_class_object($class_attributes, \%class_desc, \@objstr);
    $self->{class_object}{$class_name_in_datomic} = \@objstr;
  }
  return $self->{class_object}{$class_name_in_datomic};
}

#############################################################################

=head2 

    Title   :   parse_class_object
    Usage   :   my $model = $deuce->parse_class_object($class_attributes, $class_data, $objstr);
    Function:   returns the class model as a parsed object with an ACE model-like tree structure
    Returns :   array of class elements
    Args    :   class_attributes = array of attributes that have an ident that matches this class
                class_desc = hash-ref of class data for this class
                objstr = ref to array of hashes - populated in this method - one hash for each tag-and-value

=cut

sub parse_class_object {
  my($self, $class_attributes, $class_desc, $objstr) = @_;

  #
  # Want to grab the locatable hash defined in classes like CDS.
  # At present, locatable is the only top-level hash, but loop over all 'pace/use-ns' 
  # in case there are others in the future.
  # May wish to add a parameter to the recursive call so we can mark these tags as special
  #
  my @tophash = (exists $class_desc->{'pace/use-ns'}) ? (@{$class_desc->{'pace/use-ns'}}) : (); # e.g. ('locatable')
  foreach my $tophash (@tophash) {
    my $tophash_class_attributes = $self->get_class_attributes($tophash); # attributes of class
    # and do a bit of recursion to populate the #locatable hash tags into the current class model
    # #locatable has no definition in $classes, so just pass a null hash-ref instead of a class_desc hash-ref
    $self->parse_class_object($tophash_class_attributes, {}, $objstr);
  }

  #
  # first get the components and build the basic structure of the tree
  # of tags for this class - we do this because if we just had one
  # pass through the attributes, sub-component attributes can have no
  # 'pace/tags' names yet, so we wouldn't know where they should be on
  # the tree
  #

  foreach my $element (@{$class_attributes}) {
    if (exists $element->{'db/isComponent'}) {
      my $ident = ${$element->{'db/ident'}};
      my $ace_tags = $element->{'pace/tags'};
      my @ace_tags = split /\s+/, $ace_tags;
      $self->add_tags_to_tree($objstr, \@ace_tags, $ident);
    }
  }



  #
  # then get all the types of attributes and hang them on the tree
  #

  foreach my $element (@{$class_attributes}) {

    my $ace_tags = $element->{'pace/tags'};
    my @ace_tags = split /\s+/, $ace_tags;
    my $name = $ace_tags[-1];
    my $alternative_field_name = 0;
    my $ident = ${$element->{'db/ident'}}; # datomic name
    my $unique = (exists $element->{'db/cardinality'} && (${$element->{'db/cardinality'}} eq 'db.cardinality/one')) ? 1 : 0;
    my $type = (exists $element->{'db/valueType'}) ? (${$element->{'db/valueType'}}) : '';
    if (!defined $type || $type eq '') {$type = 'enum'}
    $type =~ s/db.type\///;
    my @hash = (exists $element->{'pace/use-ns'}) ? (@{$element->{'pace/use-ns'}}) : (); # e.g. ('evidence')
    my $isComponent = (exists $element->{'db/isComponent'}) ? 1 : 0;
    my $xref = (exists $element->{'pace/obj-ref'}) ? (${$element->{'pace/obj-ref'}}) : '';
    my $order =  (exists $element->{'pace/order'}) ? ($element->{'pace/order'}) : undef;
    my $node;
    my $remote_tag; # ACE tag of XREF link in remote class
    
    if ($type ne 'enum') {
      if (defined $name && $name ne '') {
	$node = $self->add_tags_to_tree($objstr, \@ace_tags, $ident);

	# see if this is a outgoing-xref
	if ($type eq 'ref' && $xref ne '') {
	  # find the ACE tag name in the remote class - this is
	  # overkill for most stuff that we need to do , but it proves
	  # that I understand what is going on if I can reproduce the
	  # ACE model reasonably closely
#	  print "*** Found OUTXREF $xref for $ident\n";
	  $type = 'outgoing-ref';
	  my $classes = $self->get_classes(); # hash of all classes' data
	  my @remote_class_desc_array = $classes->{$xref};
	  my %remote_class_desc = %{$remote_class_desc_array[0]};
	  my @remote_incoming_xref = @{$remote_class_desc{'pace/xref'}};
	  my $found = 0;
	  foreach my $remote_pace_xref (@remote_incoming_xref) {
	    my $remote_ace_tags = $remote_pace_xref->{'pace.xref/tags'}; # e.g. 'Refers_to CDS'
	    my @remote_ace_tags = split /\s+/, $ace_tags;
	    $remote_tag = $remote_ace_tags[-1];
	    #	print "*** OUTXREF ident = $remote_component_ident remote_tag = $remote_tag\n";
	    $found = 1;
	    last;
	  }
	}

      } else { # we have an ordered sub-component - want to search through the $objstr to find a match to the component ident
	($name) = ($ident =~ /\/(\S+)/); 
	$name = "$name";
	$alternative_field_name = 1; # name of field in line made up by Thomas
	my ($component_ident) = ($ident =~ /(\S+?)\//);
	$component_ident =~ s{(.*)\.}{$1/}xms; # change the last occurrance of . to / to get the component ident
	my $found = 0;
	for (my $i=0; $i<scalar @{$objstr}; $i++) {
	  if ($objstr->[$i]{ident} eq $component_ident) {
	    # put the sub-component in the correct position in the list of sub-components
	    while (defined $objstr->[$i]{right} && $objstr->[($objstr->[$i]{right})]{order} < $order) {
	      $i = $objstr->[$i]{right};
	    }
	    my $existing_sub_component = undef;
	    if (defined $objstr->[$i]{right}) { # the sub-component in the objstr is after the one we are inserting
	      $existing_sub_component = $objstr->[$i]{right};
	    }
	    push @{$objstr}, {name => $name, ident => $ident, down => undef, right => $existing_sub_component};
	    $node = scalar @{$objstr} - 1;
	    $objstr->[$i]{right} = $node;
	    $found = 1;
	    last;
	  }
	}
	if (!$found) {die "Didn't find component '$component_ident' when processing '$ident'.\n"}
      }
      
      # see if we wish to hide this node on an ACE display
      my $hidden = 0;
      if ($type eq 'ref' && $ace_tags eq '' && $xref eq '') {$hidden = 1}
      
      if ($type ne 'enum') {
	my ($down, $right);
	($name, $down, $right) = ($objstr->[$node]{name}, $objstr->[$node]{down}, $objstr->[$node]{right}); # get the existing values in the node
	%{$objstr->[$node]} = (
			       name      => $name,
			       alternative_field_name => $alternative_field_name,
			       hidden    => $hidden, # nothing here that we would wish to show in a ACE display
			       attribute => $element, # the original attribute data from the schema in case we need to check on it
			       ident     => $ident, # datomic identity
			       xref      => $xref, # obj-ref xref class name
			       type      => $type, # type of value
			       isUnique  => $unique, # cardinality of things it points to
			       remote_tag => $remote_tag, # name of tag in ACE model of remote class in outgoing XREF
			       isComponent => $isComponent, # true if this is a component
			       hash      => \@hash, # list of hash classes owned by this component
			       order     => $order, # pace/order - order of this tuple of fields
			       down      => $down,  # pointer to (element number of) the next down node in the tree
			       right     => $right, # pointer to (element number of) the next right node in the tree
			      );
      }
    }
  }

  
  
  #
  # Now go through the tree filling in sets of ENUM tags
  # like: ?Antibody Isolation Antigen UNIQUE ENUM Other_antigen Text
  #                                               Protein Text
  #                                               Peptide Text
  #
  #
  # Then we reorder nodes like the Text at the end of
  # ?Antibody Isolation Antigen UNIQUE ENUM Other_antigen Text
  #                                         Protein Text
  #                                         Peptide Text
  # We are now sure that we have all attributes but some pace/order > 0
  # items have been added only once when they are required to be
  # cloned at the end of each of a set of multiple ENUMs
  #

  foreach my $element (@{$class_attributes}) {
    
    my $ace_tags = $element->{'pace/tags'};
    my @ace_tags = split /\s+/, $ace_tags;
    my $name = $ace_tags[-1];
    my $alternative_field_name = 0;
    my $ident = ${$element->{'db/ident'}}; # datomic name
    my $unique = (exists $element->{'db/cardinality'} && (${$element->{'db/cardinality'}} eq 'db.cardinality/one')) ? 1 : 0;
    my $type = (exists $element->{'db/valueType'}) ? (${$element->{'db/valueType'}}) : '';
    if (!defined $type || $type eq '') {$type = 'enum'}
    $type =~ s/db.type\///;
    my @hash = (exists $element->{'pace/use-ns'}) ? (@{$element->{'pace/use-ns'}}) : (); # e.g. ('evidence')
    my $isComponent = (exists $element->{'db/isComponent'}) ? 1 : 0;
    my $xref = (exists $element->{'pace/obj-ref'}) ? (${$element->{'pace/obj-ref'}}) : '';
    my $order =  (exists $element->{'pace/order'}) ? ($element->{'pace/order'}) : undef;
    my $node;
    
    if ($type eq 'enum') {
      my ($component_ident) = ($ident =~ /(\S+?)\//);
      $component_ident =~ s{(.*)\.}{$1/}xms; # change the last occurrance of . to / to get the component ident
      my $found = 0;
      for (my $i=0; $i<scalar @{$objstr}; $i++) {
	if ($objstr->[$i]{ident} eq $component_ident) {
	  my $new_right = undef; # new node to add at the right of this ENUM node
	  my $old_right = $objstr->[$i]{right};
	  # check to see if the node to the right is pace/order > 0 - if so, then insert before it
	  if (defined $old_right && defined $objstr->[$old_right]{order} && $objstr->[$old_right]{order} > 0) {
	    $new_right = $old_right; # get what was in the ENUM column and prepare to point right to it
	    $old_right = undef; # prepare to remove the item from the end of the ENUM column
	  } elsif (defined $old_right && $objstr->[$old_right]{type} eq 'enum') { # if there is an ENUM with a pace/order > 0 node to its right, clone the end node
	    my $right_right =  $objstr->[$old_right]{right};
	    if (defined $right_right && defined $objstr->[$right_right]{order} && $objstr->[$right_right]{order} > 0) {
	      $new_right = $right_right;
	    }
	  }
	  push @{$objstr}, {name => $name, ident => $ident, down => $old_right, right => $new_right};
	  $node = scalar @{$objstr} - 1;
	  $objstr->[$i]{right} = $node;
	  $found = 1;
	  last;
	}
      }
      if (!$found) {die "Didn't find enum '$name' parent node '$component_ident' when processing '$ident'.\n"}
      
      my ($down, $right);
      ($name, $down, $right) = ($objstr->[$node]{name}, $objstr->[$node]{down}, $objstr->[$node]{right}); # get the existing values in the node
      %{$objstr->[$node]} = (
			     name      => $name,
			     alternative_field_name => $alternative_field_name,
			     hidden    => $hidden, # nothing here that we would wish to show in a ACE display
			     attribute => $element, # the original attribute data from the schema in case we need to check on it
			     ident     => $ident, # datomic identity
			     xref      => $xref, # obj-ref xref class name
			     type      => $type, # type of value
			     isUnique  => $unique, # cardinality of things it points to
			     remote_tag => undef, # name of tag in ACE model of remote class in incoming XREF
			     isComponent => $isComponent, # true if this is a component
			     hash      => \@hash, # list of hash classes owned by this component
			     order     => $order, # pace/order - order of this tuple of fields
			     down      => $down,  # pointer to (element number of) the next down node in the tree
			     right     => $right, # pointer to (element number of) the next right node in the tree
			    );
    }
  }



  # 
  # and lastly add the tags and incoming xrefs from other classes that
  # point in to this class
  #
  
  if (exists $class_desc->{'pace/xref'}) { # if we are recursing to do the #locatable there is no $class_desc
    my @incoming_xref = @{$class_desc->{'pace/xref'}};
    foreach my $pace_xref (@incoming_xref) {
      
      my $ident = ${$pace_xref->{'pace.xref/attribute'}}; # e.g. 'feature.associated-with-cds/cds'
      my $xref = ${$pace_xref->{'pace.xref/obj-ref'}}; # e.g. 'feature/id'
      my $ace_tags = $pace_xref->{'pace.xref/tags'}; # e.g. 'Associated_feature'
      my @ace_tags = split /\s+/, $ace_tags;
      my $type = 'incoming-ref';
      
      
      # how do we know whether to add an evidence hash or not?  the
      # evidence for an incoming xref is on the remote objects's
      # component so in CDS the 'feature.associated-with-cds/cds' should
      # have a #Evidence and we know this because the Feature
      # 'feature/associated-with-cds' component has a evidence, so
      # convert incoming xref ident ident to remote component ident
      # e.g. 'feature.associated-with-cds/cds' to
      # 'feature/associated-with-cds'
      
      my ($remote_component_ident) = ($ident =~ /(\S+?)\//);
      $remote_component_ident =~ s{(.*)\.}{$1/}xms; # change the last occurrance of . to / to get the component ident
      if ($remote_component_ident !~ m#\/#) {$remote_component_ident = $ident} # is this required? Does this ever occur?
      # look for the remote class's attribute in all of the attributes
      my $attributes = $self->get_attributes();
      my $found = 0;
      my @hash = ();
      my $remote_tag; # ACE tag of XREF link in remote class
      my $isComponent = 0;
      foreach my $attr (@{$attributes}) {
	if (${$attr->{'db/ident'}} =~ /^$remote_component_ident$/) {
	  @hash = (exists $attr->{'pace/use-ns'}) ? (@{$attr->{'pace/use-ns'}}) : (); # e.g. ('evidence')
	  $isComponent = (exists $attr->{'db/isComponent'}) ? 1 : 0;
	  my $remote_ace_tags = $attr->{'pace/tags'};
	  my @remote_ace_tags = split /\s+/, $remote_ace_tags;
	  $remote_tag = $remote_ace_tags[-1];
	  #	print "*** INXREF ident = $remote_component_ident remote_tag = $remote_tag\n";
	  $found = 1;
	  last;
	}
      }
      
      my $node = $self->add_tags_to_tree($objstr, \@ace_tags, $ident);
      my ($name, $down, $right) = ($objstr->[$node]{name}, $objstr->[$node]{down}, $objstr->[$node]{right}); # get the existing values in the node
      my $alternative_field_name = 0; # not a field internal to line with a name made up by Thomas
      %{$objstr->[$node]} = (
			     name      => $name,
			     alternative_field_name => $alternative_field_name,
			     hidden    => $hidden,
			     class     => $pace_xref,
			     ident     => $ident,
			     xref      => $xref, 
			     type      => $type,
			     isUnique  => 0, # how do we find if this is unique or not ? Thomas says that it is always assumed this is NOT unique but he will think of a way to specify it later. Maybe hard-code the Sequence->Sequence xrefs to be UNIQUE?
			     remote_tag => $remote_tag,
			     isComponent => $isComponent,
			     hash      => \@hash, 
			     down      => $down, 
			     right     => $right, 
			    );
      
      
    }
  }

  return;
}

#############################################################################

=head2 

    Title   :   add_tags_to_tree
    Usage   :   my $object = $deuce->add_tags_to_tree($objstr, @ace_tags, $ident);
    Function:   populate ACE non-datomic tree of tags in objstr using
                the pace/tags names to find the position to add the new node
    Returns :   int element of objstr that matches the last tag name in the array
    Args    :   $objstr - ref to array of hashes - populated in this method
                $ace_tags - arrayref of string - names of the ACE tags for this component
                $ident - string - ident to be added to the node of the last tag in @ace_tags
                $node - int element of objstr to take as the head of the sub-tree to search, or undef to search the whole tree

=cut

sub add_tags_to_tree {
  my($self, $objstr, $ace_tags, $ident, $node) = @_;
#  print "\n ****** add_tags_to_tree(@{$ace_tags}, $ident)\n";

#  if (scalar @{$ace_tags} == 0) {print "returning $node\n"} # no tag names in array 
  if (scalar @{$ace_tags} == 0) {return $node} # no tag names in array 
  my $tag = shift @{$ace_tags};

  # if this is the last tag, then add the ident to the hash
  my $thisident = '';
  if (scalar @{$ace_tags} == 0) {
    $thisident = $ident;
  }


  #
  # $node is where we found the match to the previous tag, so step right to the new head node
  #

  if (scalar @{$objstr} == 0) { # no nodes in objstr, have to add a new first node
#    print "no nodes in objstr, have to add a new first node\n";
#    print "*** push $tag  $thisident\n";
    push @{$objstr}, {name => $tag, ident => $thisident, down => undef, right => undef};
    return $self->add_tags_to_tree($objstr, $ace_tags, $ident, 0);
  }

  if (defined $node) {
    if (defined $objstr->[$node]{right}) {
#      print "going right from starting node $node\n";
      $node = $objstr->[$node]{right};
    } else { # nothing to the right, so create new node to the right
#      print "creating new node to the right\n";
#      print "*** push $tag  $thisident\n";
      push @{$objstr}, {name => $tag, ident => $thisident, down => undef, right => undef};
      my $newnode = scalar @{$objstr} - 1;
      $objstr->[$node]{right} = $newnode;
      return $self->add_tags_to_tree($objstr, $ace_tags, $ident, $newnode);
    }
  } else {
#    print "starting to search from the head node\n";
    $node = 0;
  }

  #
  # step down
  #

#  print "step down\n";
  my $lastnode = $node;
  while (defined $node && $objstr->[$node]{name} ne $tag) {
    $lastnode = $node;
    my $dummyname = $objstr->[$node]{name};
#    print "looking at node $node name = '",$objstr->[$node]{name},"'\n";
#    print "still stepping down from node $lastnode ...\n";
    $node = $objstr->[$node]{down};
#    if (defined $node) {print "next node is $node\n"}
  }

  # add node to the end of the tree
  if (!defined $node) { # off end of the tree - add a new node at the bottom
#    print "*** off end of the tree - add a new node at the bottom\n";
#    print "*** push $tag  $thisident\n";
    push @{$objstr}, {name => $tag, ident => $thisident, down => undef, right => undef};
    my $newnode = scalar @{$objstr} - 1;
    $objstr->[$lastnode]{down} = $newnode;
    return $self->add_tags_to_tree($objstr, $ace_tags, $ident, $newnode);

  } else { # found a match to $tag
#    print "found a match to $tag\n";
    return $self->add_tags_to_tree($objstr, $ace_tags, $ident, $node);
  }
}



#############################################################################
# Stuff for constructing queries
#############################################################################

=head2 

    Title   :   read_keyset_file
    Usage   :   my ($class, $Keyset_IDs) = $self->read_keyset_file($file);
    Function:   reads a Keyset file to get the class and ID names - only one class per Keyset file is allowed
                This ignores anything after the comment '//'.
                Quote characters are ignored.
                It expects a format for each line of:
                Class : ID
                Or:
                Class ID
    Returns :   $class - string datomic class name, or undef if this is not found or other problem
                $Keyset_IDs - array ref of ID names
    Args    :   $file - string - Keyset filename
                

=cut

sub read_keyset_file {
  my ($self, $file) = @_;
  my $class;
  my @Keyset_IDs;

  open (KEYSET, "<$file") || do {warn "read_keyset_file(): read_keyset_fileCan't find Keyset file '$file'\n"; return (undef, undef)};
  while (my $line = <KEYSET>) {
    chomp $line;
    $line =~ s#\/\/\.##; # remove comments
    $line =~ s#["']##g; # remove quote characters
    if ($line =~ /^\s*$/) {next} # ignore blank line
    my @line = split /\s+/, $line;
    if (defined $class) { # ckeck class is unique
      if ($class ne $line[0]) {warn "read_keyset_file(): read_keyset_fileMultiple classes in Keyset file '$file'\n"; return (undef, undef)};
    } else {
      $class = $line[0];
    }
    if (scalar @line < 2 || scalar @line > 3) {warn "read_keyset_file(): Malformed line in Keyset file '$file': $line\n"; return (undef, undef)}
    if (scalar @line == 3 && $line[1] ne ':') {warn "read_keyset_file(): Malformed line in Keyset file '$file': $line\n"; return (undef, undef)}
    if (length $line[$#line] < 1) {warn "read_keyset_file(): Malformed line in Keyset file '$file': $line\n"; return (undef, undef)}
    push @Keyset_IDs, $line[$#line];
  }
  close (KEYSET);
  my $datomic_class = $self->get_class_name_in_datomic($class);
  return ($datomic_class, \@Keyset_IDs);

}


#############################################################################
# parse a simple constraint on the current search
sub parse_cmd {
  my ($self, $input) = @_;

# the syntax is very simple, so we can parse and lex in one go
# key => regex, ignore, expect after, expect next, type, can start, can end
# this allows the novel command "FOLLOW TAG OP (NUM|STRING) 
# the word 'NEXT' (repeated as necessary) modifies TAG to get subsequent tags on an ACeDB model's line (i.e. tags in a component)
# COUNT TAG
# COUNT TAG NEXT...
# COUNT TAG OP STRING|NUM
# COUNT TAG NEXT... OP NUM
# TAG
# TAG NEXT...
# NOT TAG
# NOT TAG NEXT...
# TAG OP STRING|NUM
# TAG NEXT... OP STRING|NUM
# FOLLOW TAG
# FOLLOW TAG NEXT...
# IS STRING

  my @token_def = (
#                  Name           regex              ignore  expect after                 expect next       type      can start can end
                   [Whitespace => qr{\s+},                1, 'ALL',                       'ALL',            '',       0,        1],
                   [COUNT      => qr{\bCOUNT\b}i,         0, 'START',                     'TAG',            'COUNT',  1,        0],
                   [MISSING    => qr{(\!|\bNOT\b)}i,      0, 'START',                     'TAG',            'MISSING',1,        0],
                   [FOLLOW     => qr{\bFOLLOW\b}i,        0, 'START',                     'TAG',            'FOLLOW', 1,        0],
                   [IS         => qr{\bIS\b}i,            0, 'START',                     'STRING',         'IS',     1,        0],
                   [NEXT       => qr{\bNEXT\b}i,          0, 'TAG',                       'NEXT OP END',    'NEXT',   0,        1],
                   [GE         => qr{\>\=},               0, 'TAG NEXT',                  'NUM',            'OP',     0,        0],
                   [LE         => qr{\<\=},               0, 'TAG NEXT',                  'NUM',            'OP',     0,        0],
                   [GT         => qr{\>},                 0, 'TAG NEXT',                  'NUM',            'OP',     0,        0],
                   [LT         => qr{\<},                 0, 'TAG NEXT',                  'NUM',            'OP',     0,        0],
                   [EQ         => qr{\=},                 0, 'TAG NEXT',                  'NUM STRING',     'OP',     0,        0],
                   [NOT        => qr{(\!=|\!|\bNOT\b|\bNE\b)}i, 0, 'TAG NEXT',            'NUM STRING',     'OP',     0,        0],
                   [INT        => qr{\b[0-9]+\b},         0, 'OP',                        'END',            'NUM',    0,        1],
                   [FLOAT      => qr{\b[0-9]+[\.0-9]*\b}, 0, 'OP',                        'END',            'NUM',    0,        1], 
                   [Value_dq   => qr{\".+?\"},            0, 'OP IS',                     'END',            'STRING', 0,        1],
                   [Value_sq   => qr{\'.+?\'},            0, 'OP IS',                     'END',            'STRING', 0,        1],
                   [Value_     => qr{[\w\*]+},            0, 'OP IS',                     'END',            'STRING', 0,        1],
                   [TAG        => qr{\b[\w]+\b},          0, 'START FOLLOW COUNT MISSING','OP END',         'TAG',    1,        1],
                  );
  
  
  my @tokens;
  print "In parse_cmd: $input\n";
  
  pos($input) = 0;
  my $last = 'START';
 
  while (pos($input) < length $input) {
    my $matched = 0;
    for my $t (@token_def) {
      my ($name, $re, $ignore_flag, $expect_after, $expect_next, $type, $can_start, $can_end) = @$t;
      if (index($expect_after, $last) != -1 || $expect_after eq 'ALL') {
        if ($input =~ m/\G($re)/gc) {
          $matched = 1;
          print "in criterion: $input, match '$1' as a $name type $type\n";
          next if $ignore_flag;
          if ($last eq 'START' && !$can_start) {
            warn "parse_cmd(): There is an invalid token at the start of the command: '$input'\n'\n";
            return undef;
          }
          $last = $type;
          my $word = $1;
          push @tokens, [$name, $word, $expect_next, $type, $can_start, $can_end];
        }
      }
    }
    if (!$matched) {
      warn "parse_cmd(): Syntax error at position " . pos($input) . " of input\n$input\n".(' ' x pos($input))."^\n";
      return undef;
    }
  }

  if ($#tokens == -1) {
    warn "parse_cmd(): There are no recognised tokens in the command: '$input'\n'\n";
    return undef;
  }
  if ($tokens[-1][-1] != 1) {
    warn "parse_cmd(): There is no valid end to the command: '$input'\n'\n";
    return undef;
  }

  return \@tokens;
   
}

#############################################################################

=head2 

    Title   :   protect
    Usage   :   $pattern = $deuce->protect($pattern);
    Function:   change the wildcards in the pattern into a perl or java.util.regex.Pattern regex
    Returns :   perl regex
    Args    :   $pattern - string - optionally containing '.', '?', '*' to be protected

=cut

sub protect {

  my ($self, $pattern) = @_;

  $pattern =~ s/\./\\\\./g; # quote dots
  $pattern =~ s/\*/\.\*/g; # change * to .*
  $pattern =~ s/\?/\.\?/g; # change ? to .?

  return $pattern;
}
#############################################################################

=head2 

    Title   :   get_tag_name
    Usage   :   $tag_name_element = $deuce->get_tag_name($class, $tokens, \$next_token);
    Function:   get the class schema @{$objstr} element number of the required tag in a class
    Returns :   int array element of the class model @{$objstr} which has the required tag
                array-ref of the class model $objstr
                or 'undef' if tag is not found
    Args    :   $class = datomic name of class
                $tokens = ref to array describing tokens in the search criteria
                $next_token = ref of int of next array element in $tokens to look at

+++ Doesnt yet navigate to Evidence hash tags 


=cut


sub get_tag_name {

  my ($self, $class, $tokens, $next_token) = @_;

  my $element = undef;

  my $objstr = $self->model($class);

  # check there are more things in the $tokens array-ref starting at $next_token
  if (scalar @{$tokens} <= $$next_token) {
    warn "Expected a TAG name - it appears to be missing\n";
    return (undef, undef);
  }

  # get first tag name
  my ($tag, $tag_name) = @{$tokens->[$$next_token++]};

  # +++ check if tag name is followed by 'NEXT'

  # search through @{$objstr} for a match of the tag name to either the Ace or the Datomic tag names
  # valid tag names are: 
  #  the last word of a 'pace/tags' list
  #  the 'ident' name
  #  either of the above with a virtual tag name appended (including the '^'), e.g. 'Database^field'
  #  either of the above with a '#' then hash class/field, e.g. 'Remark#evidence/Curator_confirmed'

  my $tag_name_virtual_half = '';
  if ($tag_name =~ /(\S+)\^(\S+)/) {
    $tag_name = $1;
    $tag_name_virtual_half = "/".$2;
  }
  my $datomic_tag_name = $class.'.'.lc($tag_name).$tag_name_virtual_half;

  my $found = 0;
  for ($element = 0; $element < scalar @{$objstr}; $element++) {
    if ($tag_name eq $objstr->[$element]->{name}) {$found = 1; last} 
    if ($tag_name eq $objstr->[$element]->{ident}) {$found = 1; last} 
  }
  if (!$found) {
    warn "Can't find the tag $tag_name in $class\n";
    return (undef, undef);
  }

  # repeat:   if next token is 'NEXT' then move 'right' in objstr to the next virtual tag
  while (scalar @{$tokens} > $$next_token && $tokens->[$$next_token]->[0] eq 'NEXT') {
    $$next_token++;
    if (defined $objstr->[$element]->{right}) {
      $element = $objstr->[$element]->{right};
    } else {
      warn "Off the edge of the class model looking NEXT after tag $tag_name in $class\n";
      return (undef, undef);
    }
  }

  # adjust the element to go to the next {right} if we are on a component that is not holding an incoming-ref
  if ($objstr->[$element]->{isComponent} && $objstr->[$element]->{type} ne 'incoming-ref') {
    if (defined $objstr->[$element]->{right}) {
      $element = $objstr->[$element]->{right};
    } else {
      warn "Off the edge of the class model after going right from the Component tag $tag_name in $class\n";
      return (undef, undef);
    }    
  }


  print "\n\nFound tag '$tag_name' in class '$class' model. It has ident: '$objstr->[$element]->{ident}'\n";

  return ($element, $objstr);

}
#############################################################################
=head2 

    Title   :   ace_to_querystr
    Usage   :   my $querystr = $deuce->ace_to_querystr($class, $class_pattern, $querystr)
    Function:   convert an ACE-style query string into a datomic-like query string
    Returns :   datomic query string or undef if error
                datomic args string formed from any keyset file specified as the class 'Keyset'
    Args    :   $class = datomic name of class
                $class_pattern = optionally wildcarded pattern of ID in class to search for first, or name of Keyset file if class is 'Keyset'
                $criteria = any subsequent criteria to search for 
                $classref - ref to string - returns the class of object returned, if defined

=cut

sub ace_to_querystr {
  my ($self, $class, $pattern, $criteria, $classref) = @_;

  # [:find ?col1-id = ?col-id${class_var} to get a unique entity col-id that changes with the output class
  # $class_var = name of the class to output
  # col2-id, col3-id = col${criterion_count}-id  local criteria variable
  # ?col1 = ?${class_var} if we consistently use the class name as also being the variable for output then this keeps things easy
  # ?col2, ?col3 etc = ?col${criterion_count} local criteria variable


  my $result=undef; # the result of the query
  my $criterion_count = 0; # the next numeric suffix to use on variable identifiers
  my $keyset = 0; # flag for have a keyset
  my $args = ''; # set of IDs in keyset;
  my $Keyset_IDs;

  if (!defined $class || $class eq '') {
    return (undef, undef);
  }

  if ($class eq 'Keyset') { # should this be a case-independent match?
    $keyset = 1; # set the flag to say we have a keyset
    my $file = $pattern;
    $pattern = '*'; # want all of the Keyset IDs
    if (! -e $file) {warn "Can't find Keyset file '$file'\n"; return (undef, undef)}
    ($class, $Keyset_IDs) = $self->read_keyset_file($file);
    if (!defined $class) {warn "Problem reading data in Keyset '$file'\n"; return (undef, undef)}
  }
  if (!defined $pattern || $pattern eq '') {
    $pattern = '*';
  }

  if (!defined $criteria) {
    $criteria = '';
  }

  my $class_var = lc "${class}"; # initialise the class_variable to be the current lowerclassed class name (use wherever colonnade has '?col1, ?col6 etc.)
  if (defined $classref) {$$classref = $class_var}
  my $in = ':in $ ';
  my $where = ":where ";
  my $find;
  my $tag_name_element;
  my $op;

  print "In ace_to_querystr, Criteria: $criteria\n";

  # initialise the col-id - return a collection of names
  $find = ":find [?col-id${class_var} ...]\n"; 

  # check if we have a keyset
  if ($keyset) {
    $in = ':in $ '."[?col-id${class_var} ...]\n";
    $where .= "[?${class_var} :${class_var}/id ?col-id${class_var}]\n";
    $args = join '" "', @{$Keyset_IDs};
    $args = '["'.$args.'"]';

  } else {

    # if not a keyset, start off by constructing a query to get the class and pattern
    # is the pattern a regular expression?
    if ($pattern =~ /\*/ || $pattern =~ /\?/) {
      # [:find ?col1-id :where [?col1 :cds/id ?col1-id] [(re-pattern "AC3.*") ?col1-regex] [(re-matches ?col1-regex ?col1-id)]] 
      $pattern = $self->protect($pattern);
      
      $where .= "\n";
      $where .= "[?${class_var} :${class_var}/id ?col-id${class_var}]\n";
      $where .= "[(re-pattern \"(?i)$pattern\") ?var${criterion_count}-regex]\n"; # case-independent regular expression
      $where .= "[(re-matches ?var${criterion_count}-regex ?col-id${class_var})]\n";
      
    } else {
      # [:find ?col1-id :where [?col1 :cds/id ?col1-id] [(ground "AC3.3") ?col1-id]]
      # a single non-regex value
      $where .= "\n";
      $where .= "[?${class_var} :${class_var}/id ?col-id${class_var}]\n";
      $where .= "[(ground \"$pattern\") ?col-id${class_var}]\n";
    }
  }
  my @criteria = split /\;|\bAND\b/, $criteria;

  foreach my $criterion (@criteria) {
    if ($criterion =~ /^\s*$/) {next}

    my $tokens = $self->parse_cmd($criterion);
    if (!defined $tokens) {return (undef, undef)} # syntax error found
    $criterion_count++; # for making variables that are local to this criterion
    
    # get description of first word of command 
    #       Name       Word            Expect Type       Start End
    # e.g. ('MISSING', '(\!|\bNOT\b)', 'TAG', 'MISSING', 1,    0)
    my $next_token = 0;
    my ($name, $word, $expect_next, $type, $can_start, $can_end) = @{$tokens->[$next_token]};

    if ($can_start) {

      if ($type eq 'TAG') { 
	# find objects where the TAG matches various conditions
	$self->TAG_command($word, $criterion_count, $class, $tokens, $next_token, $class_var, $criterion);
      } elsif ($type eq 'COUNT') {
	$self->COUNT_command($word, $criterion_count, $class, $tokens, $next_token, $class_var, $criterion);
      } elsif ($type eq 'MISSING') { 
	# '! TAG' or 'NOT TAG' - find objects where the TAG or 'TAG OP VALUE' doesn't exist
	$self->MISSING_command($word, $criterion_count, $class, $tokens, $next_token, $class_var, $criterion);
      } elsif ($type eq 'FOLLOW') { 
	# expect a incoming or outgoing xref and change the class to match it and find instances of the new class that xref the existing class
	my ($FOLLOW_class_var, $FOLLOW_where) = $self->FOLLOW_command($word, $criterion_count, $class, $tokens, $next_token, $class_var, $criterion);
	$class_var = $FOLLOW_class_var;
	$find = ":find [?col-id${class_var} ...]\n"; 
	if (defined $FOLLOW_where) {$where .= $FOLLOW_where} else {return undef}
      } elsif ($type eq 'IS') { 
	# find resulting objects whose name matches the STRING
	my $IS_where .= $self->IS_command($word, $criterion_count, $class, $tokens, $next_token, $class_var, $criterion);
	if (defined $IS_where) {$where .= $IS_where} else {return undef}
      } else {
        warn "ace_to_querystr(): Command '$word' not recognised\n";
        return (undef, undef);
      } # list of commands
    } else { # can start
      warn "ace_to_querystr(): Command '$word' not recognised\n";
      return (undef, undef);      
    }
  } # foreach my $criterion (@criteria)

  my $query = '[' . $find . $in . $where . ']' ;

  return ($query, $args);
}

#############################################################################

=head2 

    Title   :   OP
    Usage   :   my $opstr = $deuce->OP($OP);
    Function:   convert any of the ways an operator may be written into the cononical form required
    Returns :   canonical form of the OP string
    Args    :   an OP string written by the user

=cut

sub OP {

  my %ops = (
             'EQ' => '=',
	     '=' => '=',

             'NOT' => '!=',
	     '!=' =>  '!=',
	     '!' =>   '!=',

             'GE' => '>=',
	     '>=' => '>=',

             'LE' => '<=',
	     '<=' => '<=',

             'GT' => '>',
	     '>'  => '>',

             'LT' => '<',
	     '<' =>  '<',
            );

  return $ops{$OP};

}

#############################################################################

=head2 

    Title   :   TAG_command
    Usage   :   my $querystr = $deuce->TAG_command();
    Function:   Construct a TAG query. The tag should exist or it should match the OP VALUE constraint.
    Returns :   datomic query string or undef if error
    Args    :   

=cut

sub TAG_command {
  my ($self, $word, $criterion_count, $class, $tokens, $next_token, $class_var, $criterion) = @_;

  

}

#############################################################################

=head2 

    Title   :   COUNT_command
    Usage   :   my $querystr = $deuce->COUNT_command();
    Function:   Construct a COUNT query. The following TAG should match the OP NUM constraint
    Returns :   datomic query string or undef if error
    Args    :   

=cut

sub COUNT_command {
  my ($self, $word, $criterion_count, $class, $tokens, $next_token, $class_var, $criterion) = @_;


  

}

#############################################################################

=head2 

    Title   :   MISSING_command
    Usage   :   my $querystr = $deuce->MISSING_command();
    Function:   Construct a MISSING query. The following TAG should not exist, or the TAG OP VALUE should not exist.
    Returns :   datomic query string or undef if error
    Args    :   

=cut

sub MISSING_command {
  my ($self, $word, $criterion_count, $class, $tokens, $next_token, $class_var, $criterion) = @_;

  

}

#############################################################################

=head2 

    Title   :   FOLLOW_command
    Usage   :   my ($new_class, $where) = $deuce->FOLLOW_command();
    Function:   Construct a FOLLOW query. Follow the xref and change the class we return.
    Returns :   new 'class' name string and 'where' clause or undef if error
    Args    :   

=cut

sub FOLLOW_command {

  my ($self, $word, $criterion_count, $class, $tokens, $next_token, $class_var, $criterion) = @_;

  my $new_class;
  my $where;

# +++ working on this 5th Oct 2015
# +++ need to cope with 'locatable/method'

  # FOLLOW should cause the $class_var to change
  # FOLLOW TAG (NEXT)
  $next_token++;
  if ($next_token >= scalar @{$tokens}) {
    warn "FOLLOW_command(): Expected something after '$word' in '$criterion'\n";
    return (undef, undef);
  }

  my ($element, $objstr) = $self->get_tag_name($class, $tokens, \$next_token);
  if (defined $element) {
    
#    my ($follow_where, $value_used) = $self->construct_where_line($previous_entity, $element, $objstr);
#    $where .= $follow_where;

  } else {
    warn "Unknown type of tag '$word' in '$criterion'\n";
    return (undef, undef);
  }


  return ($new_class, $where);
}

#############################################################################

=head2 

    Title   :   IS_command
    Usage   :   my $querystr = $deuce->IS_command();
    Function:   Construct an IS query. Constrain the class ID name to match the following pattern.
    Returns :   datomic query string or undef if error
    Args    :   

=cut

sub IS_command {
  my ($self, $word, $criterion_count, $class, $tokens, $next_token, $class_var, $criterion) = @_;

  $next_token++;
  if ($next_token >= scalar @{$tokens}) {
    warn "IS_command(): Expected something after '$word' in '$criterion'\n";
    return undef;
  }

  my ($next_name, $next_word, $next_expect_next, $next_type, $next_can_start, $next_can_end);
  ($next_name, $next_word, $next_expect_next, $next_type, $next_can_start, $next_can_end) = @{$tokens->[$next_token]};

  print "parsed command is: $word $next_word\n";
  if ($next_type ne 'STRING') {
    warn "IS_command(): Expected string after '$word' in '$criterion'\n";
    return undef;            
  }
  if ($next_word =~ /\*/ || $next_word =~ /\?/) {
    # [:find ?col1-id :where [?col1 :cds/id ?col1-id] [(re-pattern "AC3.*") ?col1-regex] [(re-matches ?col1-regex ?col1-id)]] 
    $next_word = $self->protect($next_word);
    $where .= "[?${class_var} :${class_var}/id ?col-id${class_var}]\n";
    $where .= "[(re-pattern \"(?i)$next_word\") ?var${criterion_count}-regex]\n"; # case-independent regular expression
    $where .= "[(re-matches ?var${criterion_count}-regex ?col-id${class_var})]\n";
  } else {
    # [:find ?col1-id :where [?col1 :cds/id ?col1-id] [(ground "AC3.3") ?col1-id]]
    # a single non-regex value
    $where .= "[?${class_var} :${class_var}/id ?col-id${class_var}]\n";
    $where .= "[(ground \"$next_word\") ?col-id${class_var}]\n";
  }

  return $where;

}

#############################################################################

=head2 

    Title   :   construct_where_line {
    Usage   :   my $querystr = $deuce->construct_where_line($previous_entity, $element, $objstr);
    Function:   Construct a set of where lines. Updates the hash of idents => entity names used.
    Returns :   new where lines and the name of the value for other lines to use and name of new class in case it has changed (used in a FOLLOW)
    Args    :   

=cut


sub construct_where_line {

  my ($self, $previous_entity, $element, $objstr) = @_;
  
  my $new_value;
  my $new_class;

  my $type = $objstr->[$element]->{type};
  my $ident = $objstr->[$element]->{ident};
  my $xref =  $objstr->[$element]->{xref};

  if ($type eq 'incoming-ref') { # this may be a component holding details of the remote ident plus hash objects
    print "incoming xref ident = $ident, xref = $xref";
    
# example of self-referential xref Gene -> Gene. Here we can't simply
# start a hash of idents and names used for the entities because the
# class is the same

    #   157  HASH(0x2312b60)
    #      'class' =>          
    #         'db/id' => 17592186045452
    #         'pace.xref/attribute' =>             -> 'gene/hide-under'
    #         'pace.xref/obj-ref' =>             -> 'gene/id'
    #         'pace.xref/tags' => 'Map_info Representative_for'
    #      'ident' => 'gene/hide-under'
    #      'isComponent' => 0
    #      'remote_tag' => 'Hide_under'
    #      'type' => 'incoming-ref'
    #      'xref' => 'gene/id'

#    	  '[:find ?pheno (distinct ?ph)
#	    :in $ ?gid
#	    :where [?g :gene/id ?gid]             # get the Gene object entity, given the Gene name
#	           [?gh :rnai.gene/gene ?g]       # in class Gene, RNAi is an INXREF - the rnai.gene/gene is a ref holding the entity of the Gene obj
#	           [?rnai :rnai/gene ?gh]         # go back a level to the component to get the RNAi object entity
#	           [?rnai :rnai/phenotype ?ph]    # in class RNAi, Phenotype is an OUTXREF
#	           [?ph :rnai.phenotype/phenotype ?pheno]])

    # [?${previous_entity} :gene/hide-under ?ref-entity]
    # [?ref-entity :gene/id ?${new_value}]
    # $new_class = $xref;
    
  } elsif ($type eq 'outgoing-ref') {
    print "outgoing xref ident = $ident, xref = $xref";
    

    #  116  HASH(0x23091d0)
    #      'attribute' => HASH(0x23bc720)
    #         'db/cardinality' =>             -> 'db.cardinality/many'
    #         'db/id' => 323
    #         'db/ident' =>             -> 'gene/reference'
    #         'db/valueType' =>             -> 'db.type/ref'
    #         'pace/obj-ref' =>             -> 'paper/id'
    #         'pace/tags' => 'Reference'
    #      'ident' => 'gene/reference'
    #      'isComponent' => 0
    #      'remote_tag' => 'Reference'
    #      'type' => 'outgoing-ref'
    #      'xref' => 'paper/id'
    
    # [?${previous_entity} :gene/reference ?${ref-entity}]
    # [?${ref-entity} :paper/id ?${new_value}]
    # $new_class = $xref;
    
    # want to update the ${class_var} entity variable - this links all criteria for the class we now expect
    # get the new class      
    # my $new_class = whatever 
    # initialise the class_variable to be the new lowerclassed class name
    $new_class = lc "${new_class}"; 
    
  } elsif ($type eq 'long') {

  } elsif ($type eq 'double') {

  } elsif ($type eq 'float') {

  } elsif ($type eq 'string') {

  } elsif ($type eq 'instant') {

  } elsif ($type eq 'ref') { # NB the nodes that are not components with a ref to enums don't have object names

  } elsif ($type eq 'enum') {

  } else {
#    warn "construct_where_line(): The type of tag in '$criterion' is $type - can't construct where lines for it\n";
    return (undef, undef, undef);
  }

  return ($where, $new_value, $new_class)

}

#############################################################################
#############################################################################
#############################################################################
#############################################################################

#############################################################################
# ACE - like routines - these try to stick fairly closely to the old Ace methods
# See: /software/worm/lib/site_perl/5.16.2/Ace.pm
#############################################################################

=head2 

    Title   :   fetch
    Usage   :   @results = $deuce->fetch('CDS', 'AC3.3*', 'Method = curated', \$count, \$returned_class)
    Function:   gets a list of the names of the members of the specified class that match the patetrn and the optional query criteria
    Returns :   number of results or sorted ID names of matching objects in array.
    Args    :  $class - string - the class of object to get (datomic or Ace names can be used), Also includes the special "classes" 'Class' and 'Keyset'
               $pattern - string - pattern to match to ID name of the required object (or Keyset file if class is 'Keyset')
               $query - string - criteria to restrict the class/pattern results
               $countref - ref to int - returns the count of objects (optional)
               $classref - ref to string - returns the datomic name of the class of object returned (optional)

=cut

sub fetch {

  my ($self, $class, $pattern, $query, $countref, $classref) = @_;

  $pattern ||= '*';

  my ($count, @results);
  if (defined $countref) {$$countref = 0};
  if (defined $classref) {$$classref = $class};

  # print "in fetch() class name = $class\n";

  if ($class =~ /Class/i) { # case-independent match
    $self->get_classes();
    my @classes = keys %{$self->{classes}};
    $pattern = $self->protect($pattern);
    @results = sort {$a cmp $b} grep /$pattern/i, @classes;
    $count = scalar @results;

  } else {

    my $datomic_class = $self->get_class_name_in_datomic($class);
    if (!defined $datomic_class) {warn "fetch(): Can't find datomic name of class '$class'\n"; return ()}

    my ($querystr, $queryargs) = $self->ace_to_querystr($datomic_class, $pattern, $query, undef, $classref);
    if (defined $querystr) {
      print "query: $query\ndatomic: $querystr\n";
      (@results) = $self->query($querystr, $queryargs);
      @results = @{$results[0]};
      @results =  sort {$a cmp $b} @results;
      $count = scalar @results;
      if (defined $countref) {$$countref = $count}
    }
  }

  return $count if !wantarray;
  if ($count) {
    return @results;
  } else {
    return ();
  }
}




#############################################################################
#############################################################################
#############################################################################
#############################################################################

1;

__END__


TAG exists

[:find ?col1-id :where 
       [?col1 :cds/id ?col1-id] 
       [?col1 :cds/source-exons ?col2]]

[:find ?name :where 
       [?e :cds/id ?name]
       [?e :cds/from-laboratory _]]

# TAG OP STRING|NUM

# FIND CDS; Species = "Caenorhabditis elegans" (ID of object)

[:find ?col1-id :where 
       [?col1 :cds/id ?col1-id] 
       [?col1 :cds/species ?col2] 
       [?col2 :species/id ?col2-id] 
       [(ground "Caenorhabditis elegans") ?col2-id]]

# FIND CDS; Remark = "gw3" (simple string)

[:find ?col1-id :where 
       [?col1 :cds/id ?col1-id] 
       [?col1 :cds/remark ?col2] 
       [(ground "gw3") ?col2]]

# TAG EQ "*e"

[?col1 :cds/brief-identification ?col6] 
[(re-matches ?col6-regex ?col6)]
[(re-pattern ".*e") ?col6-regex] 

# TAG EQ "AC3.3"

[:find ?col1-id :where 
       [?col1 :cds/id ?col1-id] 
       [(ground "AC3.3") ?col1-id]]


# TAG NOT "gw3"

[:find ?col1-id :where 
	[?col1 :cds/id ?col1-id] 
       	(not [?col1 :cds/remark ?col2] 
       	     [(ground "gw3") ?col2])]

## add
## (not CLAUSE)


# TAG NOT "AC3*"

[:find ?col1-id :where 
	 [?col1 :cds/id ?col1-id] 
	 [?col1 :cds/tag-name ?col6] 
	 (not [(re-matches ?regex ?col6)]
	      [(re-pattern "AC3.*") ?regex])
] 

## add
## (not CLAUSE)


## COUNT TAG OP INT

[(datomic.api/q					<- yes, this is part of the query
	'[:find ?col-id (count ?countvar)
	:where [?local :${class_var}/id ?col-id]
	       [?local :${ident} ?countvar]
	]
	$)
[[?col-id ?count]]]
[(OP ?count INT)]]

e.g. Find genes annotated with exactly 2 GO terms
   [:find ?gene-id
    :where [(datomic.api/q
              '[:find ?gene-id (count ?go-term)
                :where [?gene :gene/id ?gene-id]
                       [?gene :gene/go-term ?go-term]]
               $)
            [[?gene-id ?go-count]]]
           [(= ?go-count 2)]]


------------------------------------------------------------------------------------------------------
------------------------------------------------------------------------------------------------------
------------------------------------------------------------------------------------------------------
------------------------------------------------------------------------------------------------------

Get object description
Get TAG clauses
Add OP VALUE clauses to TAG clauses
Wrap the result as per COUNT, MISSING, FOLLOW, IS
Prepend the object description (which may have been modified by a FOLLOWS)

start with object description
=============================

[:find ?object-name :where 
[?object-entity :object/id ?object-name]

make TAG clauses
================

# use a hash of {ident => ?tag-value name} to preserve and re-use
#  tag-value names of fields within a component, things like FIND CDS;
#  Database=UniProt; Acession=XYZ are then restricted to the same ACE
#  line. This hash should be deleted when doing a FOLLOW. The idents
#  of the current class only should be stored, not OUTXREF idents.

# If simply testing if the tag exists, then no further clauses need be added after this

simple tag format
-----------------

[?object-entity :object/tag-name ?tag-value-1] <- :object/tag-name is the tag's ident, the name '?tag-value' should be local to this criterion

enum
----

find the parent component's ident by stripping off the component part
from the tag's ident - repeat as many times as required (adding clauses
as you go) to get to the base component

[?object-entity :object/tag-name ?ref-tag-value-1] <- this is the parent component. :object/tag-name is the parent component's ident
[?ref-tag-value-1 :object.comp/tag-name ?tag-value-1] <- :object.comp/tag-name is the tag's ident, the names '?tag-value' and '?ref-tag-value' should be local to this criterion

component sub-tag simple value
------------------------------

As enum, above.

OUTXREF
-------
         
Like the CDS class in ACE:
Reference ?Paper OUTXREF CDS

and the attributes array has:
         'db/cardinality' => 'db.cardinality/many'
         'db/id' => 1241
         'db/ident' => 'cds/reference'
         'db/valueType' => 'db.type/ref'
         'pace/obj-ref' => 'paper/id'
         'pace/tags' => 'Visible Reference'

and the PAPER/ID schema class array element contains
               'db/id' => 17592186045733
               'pace.xref/attribute' => 'cds/reference'
               'pace.xref/obj-ref' => 'cds/id'
               'pace.xref/tags' => 'Refers_to CDS'



If the tag ident :object.*/tag-name is in a component, find the parent
component's ident by stripping off the component part of the tag's
ident - repeat as many times as required (adding clauses) to get to
the base component, adding clauses as you go.
 
[?object-entity :object/tag-name ?ref-tag-value-1] <- :object/tag-name (e.g. 'cds/reference') is the tag's ident
[?ref-tag-value-1 :object2/id ?tag-value-1] <- :object2/id is the foreign objects's ident (usually 'class/id') as specified in 'pace/obj-ref', the names '?tag-value' and '?ref-tag-value' should be local to this criterion


Like the CDS class in ACE:
Corresponding_protein UNIQUE ?Protein OUTXREF Corresponding_CDS #Evidence 

and the attributes array has the component:
         'db/cardinality' => 'db.cardinality/one'
         'db/id' => 1233
         'db/ident' => 'cds/corresponding-protein'
         'db/isComponent' => REUSED_ADDRESS
         'db/valueType' => 'db.type/ref'
         'pace/tags' => 'Visible Corresponding_protein'
         'pace/use-ns' => ARRAY(0x2bf40f8)
            0  'evidence'

and the attribute for the tag:
         'db/cardinality' => 'db.cardinality/one'
         'db/id' => 1234
         'db/ident' => 'cds.corresponding-protein/protein'
         'db/valueType' => 'db.type/ref'
         'pace/obj-ref' => 'protein/id'
         'pace/order' => 0
         'pace/tags' => ''

and the PROTEIN/ID schema class array element contains
               'db/id' => 17592186045729
               'pace.xref/attribute' => 'cds.corresponding-protein/protein'
               'pace.xref/obj-ref' => 'cds/id'
               'pace.xref/tags' => 'Visible Corresponding_CDS'

so this should be:
[?object-entity :object/tag-name ?ref-value-1] <- :object/tag-name (e.g. 'cds/corresponding-protein') is the tag component's ident
[?ref-value-1 :object.comp/tag-name ?ref-tag-value-1] <- :object.comp/tag-name (e.g. 'cds.corresponding-protein/protein') is the tag's ident
[?ref-tag-value-1 :object2/id ?tag-value-1] <- :object2/id is the foreign objects's ident (usually 'class/id') as specified in 'pace/obj-ref', the names '?tag-value', '?ref-value' and '?ref-tag-value' should be local to this criterion



hash
----

Find the tag's parent component's ident by stripping off the component
part of the tag's ident from the tag used as a base for the #evidence
(or whatever hash name used) until you get to the base component that
has the name of the hash in 'use-ns') to get to the base
component. There is no need to add these clauses until you gt to the
parent component because that is the one that has the ref to the hash.

If the tag ident :hash-name.*/tag-name is in a component, find the
hash parent component's ident by stripping off the component part of
the tag's ident - repeat as many times as required (adding clauses as
you go)

[?object-entity :object/tag-name ?ref-tag-value-1] <- this is the parent component with the 'use-ns' array. :object/tag-name is the parent component's ident
[?ref-tag-value-1 :hash-name/tag-name ?hash-value-1] <- :hash-name/tag-name is the tag's ident, the names '?hash-value' and '?ref-tag-value' should be local to this criterion

Most hash tags are OUTXREFS to other object/ids, so you then have to produce the hash-tag clause:
[?hash-value-1 :object2/id ?object2-value-1]   <- :object2/id is the class the hash tag points to with the entity ref ?tag-value-1. '?object2-value-1' should be local

Or if its the text value, then simply use '?hash-value-1' as the binding value.

Or if it is in a component (e.g. for
'evidence.author-evidence/author') , then find your way up to the
required hash tag, outputting the clauses as you go.

[?ref-hash-value-1 :hash-name/author-evidence ?ref-hash-value-2]
[?ref-hash-value-2 :hash-name.author-evidence/author ?ref-hash-value-3]
[?ref-hash-value-3 :author/id ?object2-value-1]

It gets better.

If the hash is on an incoming xref, then the hash is pointed to by the component that holds the remote object.
So follow the xref back to the remote object's tag.
Then go back to the remote component.
Then xref to the hash/id parent object then follow down to the required tag.
Then, if it is an outxref, get the ref to get the the remoter object/id value


locatable (class hash)
----------------------

Check the locatable object is in 'pace/use-ns'
Then get the required tag value as normal.

sort of like this example from TD showing use of 'pull'
    '[:find ?cds-id (pull ?cds [:cds/source-exons]) ?seq-id
      :where [?cds :cds/id ?cds-id]
             [(ground "AC3.3") ?cds-id]
             [?cds :locatable/parent ?seq]
             [?seq :sequence/id ?seq-id]]


Locatable has this structure as a set of enums, longs and xrefs:
locatable/parent -> sequence/id
locatable/min long 
locatable/max long
locatable/strand -> enum locatable.strand/positive
		 -> enum locatable.strand/negative
locatable/method -> method/id
locatable/score float

So we treat it like a normal hash but hanging off the class/id object

[?object-entity  :hash-name/tag-name ?tag-value-1] <- get the locatable/tag-name value, e.g. min, max, score value or a ref

Then if the value is a ref, do one of:

[?tag-value-1 :sequence/id ?seq-name] <- get the sequence name

[?tag-value-1 :method/id ?method-name] <- get the method name

and for the enums, 
[?tag-value-1 :locatable.strand/positive _] <- specify we want the positive enum





INXREF
------

??? not sure how to do this stuff


Like the CDS class in ACE:
Expr_pattern ?Expr_pattern INXREF CDS

with the CDS class array element:
               'db/id' => 17592186045443
               'pace.xref/attribute' => 'gene.corresponding-cds/cds'
               'pace.xref/obj-ref' => 'gene/id'
               'pace.xref/tags' => 'Visible Gene'


[?object-entity :object/tag-name ?tag-value-1] <- :object/tag-name is the tag's ident (), the name '?tag-value' should be local to this criterion

[?exons-ref :gene.corresponding-cds/_cds  ?geneid]
[?gene-ref :gene/_corresponding-cds ?geneid]

??? not sure how to do this stuff


OP VALUE clauses
================

# EQUALS VALUE
[(ground VALUE) ?tag-value] <- VALUE is string or number

# EQUALS REGEX STRING
[(re-matches ?regex ?tag-value-1)] <- the name '?regex' should be local to this criterion
[(re-pattern REGEX) ?regex]  <- REGEX is regex string line "e.*"

# NOT, NE
Treat it as EQUALS, then negate, see NEGATION above, or we could just use the '!=' OP

# GE, LE, GT, LT, >=, <=, >, <          <- translate these to the required form using the %ops hash
[(OP ?tag-value ?value-1)]              <- where OP is >=, <=, >, <, the name '?value should be local to this criterion
[(ground VALUE) ?value-1]               <- VALUE is number and is bound to ?value in above clause



MISSING
=======

# NOT TAG

When there is no OP VALUE, just testing for the absence of a tag
[(missing? $ ?object-entity :object/tag-name)]]     <- what about xrefs, do we want the component name here???? what about incoming xrefs???

NEGATION
========

(not CLAUSES) <- not negates the whole set of CLAUSES
     	      <- NOT TAG = 2 any object with this TAG value is rejected
     	      <- as opposed to TAG != 2 any object that has a TAG value that is not 2 is accepted, even if it also has a TAG = 2 - I think we should generally not try to search for this, just search for not(TAG = 2) to exclude any where this matches


COUNT
=====

Probably easier to do this individually as it is not a simple case of
adding the OP VALUE cluases and wrapping it in anything else that
needs to be added.

Can probably get the TAG clauses as normal, then wrap them like so, with the OP INT at the end:
    	   [(datomic.api/q
              '[:find ?object-name (count ?tag-value-1)		 <- the name '?tag-value' should be local to this criterion
	      :where
	      TAG CLAUSES <- including normal local name '?tag-value' to link to COUNT stuff above
		]
               $)
            [[?object-name ?count-1]] <- these OP VALUE clauses should probably be done bespoke for COUNT, the name '?count' should be local to this criterion
	   ]
          [(OP ?count-1 VALUE)] <- second OP VALUE clause



FOLLOW
======

# The hash of {ident => ?tag-value name} should be deleted after doing the FOLLOW

IS
==

