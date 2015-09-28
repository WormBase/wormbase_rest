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

  use Deuce;

  my $db = Deuce->new();
  my $results = $db->$query($datomic_querystr, $queryargs);
  my $schema = $db->schema;
  my $object = $db->fetch_object($class, $id);
  my @ID_names = $db->fetch($class, $class_id_pattern);
  my @ID_names = $db->fetch($class, $class_id_pattern, $tace_command_filter);

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

use lib $ENV{'CVS_DIR'};
use Carp;
use warnings;
use edn;          # Thomas Down's module for wrapping data in EDN
use Getopt::Long;
use HTTP::Tiny;
use HTTP::Tiny::Multipart;
use Data::Dumper;
use Config::IniFiles;


=head2

    Title   :   new
    Usage   :   my $db = Deuce->new();
    Function:   initialises the connection to the datomic REST server
    Returns :   Deuce object;
    Args    :   name - text - db name (as stored in the ~/.deuce/CERTIFICATE.s INI file)

=cut

sub new {
  my $class = shift;
  my $self = {};
  bless $self, $class;

  my $name = shift;
  
  my ($url, $cert, $key, $pw) = $self->get_cert_file($name);

  $self->{'url'} = $url;

  $self->{client} = HTTP::Tiny->new(
				    max_redirect => 0, 
				    SSL_options => {
						    SSL_cert_file  => $cert, 
						    SSL_key_file   => $key,
						    SSL_passwd_cb  => sub { return $pw; }
						   });
  return $self;
  
}

#############################################################################

=head2

    Title   :   get_db_ini_file
    Usage   :   $ini = $self->get_db_ini_file()
    Function:   Gets the object for the INI config file
    Returns :   ref to INI object, file path
    Args    :   

=cut


sub get_db_ini_file {
  my ($self) = @_;

  my $path = glob("~/.deuce/");
  if (!-e $path) {mkdir $path};
  my $db_ini_file = "$path/CERTIFICATE.s";
  my $ini = Config::IniFiles->new( -file => $db_ini_file, -allowempty => 1);
  return ($ini, $db_ini_file);
}

#############################################################################

=head2 
  
  Title   :   get_cert_file
  Usage   :   $self->get_cert_file();
  Function:   save to a file the path to your SSL certificate and key files and later retrieve it
  Returns :   certificate file path, key file path, password
  Args    :   text - (optional) name of database

=cut

sub get_cert_file {
  my ($self, $name) = @_;
  
  my ($cert, $key, $pw);
  
  my ($ini, $path) = $self->get_db_ini_file();

  if (!defined $name || $name eq '') {
     my @sections = $ini->Sections;
     die "DB name not specified.\nKnown DB names are: @sections\nTo store details on a new name, you should specify a new name.";
  }

  if ($ini->SectionExists($name)) {
    $url = $ini->val($name, 'URL');
    $cert = $ini->val($name, 'CERTIFICATE');
    $key = $ini->val($name, 'KEY');
    $pw = $ini->val($name, 'PASSWORD');

  } else {    
    print "Database '$name' not known, please enter details...\n";
    print "Enter URL of machine [http://db.wormbase.org:8120] > ";
    do {
      $url = <STDIN>;
      chomp $url;
#  'https://curation.wormbase.org:8131'; # SSL version of geneace
      if ($url eq '') {$url = 'http://db.wormbase.org:8120'}
    } while ($url =~ /^http[s]*\:\/\/\S+?\.wormbase.org:\d+$/); # e.g. http://db.wormbase.org:8120
    
    do {
      print "Enter full path of your SSL certificate .pem file > ";
      $cert = <STDIN>;
      chomp $cert;
      $cert = glob $cert;
    } while (! -e $cert);
    do {
      print "Enter full path of your SSL key .pem file > ";
      $key = <STDIN>;
      chomp $key;
      $key = glob $key;
    } while (! -e $key);
    print "Enter password for your SSL .pem files > ";
    $pw = <STDIN>;
    chomp $pw;

    $ini->AddSection ($name);
    $ini->RewriteConfig;

    $ini->newval($name, 'URL', $url);
    $ini->newval($name, 'CERTIFICATE', $cert);
    $ini->newval($name, 'KEY', $key);
    $ini->newval($name, 'PASSWORD', $pw);
    $ini->RewriteConfig;

    chmod 0600, $path;
    print "Details have been stored in $path\n";
  }
  
  return ($url, $cert, $key, $pw);
}

#############################################################################

=head2 

    Title   :   edn_post
    Usage   :   my $results = $db->$edn_post($uri, $content)
    Function:   post an EDN string to the database and get the result - used by a couple of query-type methods here
    Returns :   array of arrays holding the result of the query or transaction
    Args    :   $uri - string - URI of the server
                $content - string - EDN query or transaction

=cut

sub edn_post {
    my ($self, $uri, $content) = @_;
    my $resp = $self->{client}->post($uri, {
        content => $content,
        headers => {
            'content-type' => 'application/edn'
        }
    });
    die "Failed to connect to nameserver $resp->{'content'}" unless $resp->{'success'};
    return edn::read($resp->{'content'});
}

#############################################################################

=head2 

    Title   :   query
    Usage   :   my $results = $db->$query($querystr, $queryargs)
    Function:   get the results of a datomic query 
    Returns :   int count of total number of results, not constrained by the $max_rows
                array of arrays holding the result of the first $max_rows results of the query
    Args    :   q - string - datomic query
                params - string - optional list of arguments for the query (e.g. IDs from a Keyset:  '["WBGene00000016" "WBGene00000017"]')

=cut

sub query {
  my $self = shift;
  my $q = shift;
  my $params = [ {'db/alias' => 'x'}, @_ ];
  
  $q = EDN::Literal->new($q) unless ref($q);
  
  my $post = {
	      'q' => $q
	     };
  
  if (scalar(@$params) > 0) {
    $post->{'args'} = $params;
  };
  
  $self->edn_post(
		  "$self->{url}/api/query", 
		  edn::write($post)
		 );
}

#############################################################################

=head2 

    Title   :   transact
    Usage   :   my $results = $db->$transact($txn, $opts)
    Function:   do a transaction to change data
    Returns :   results
    Args    :   $transaction - edn string of transaction
                $opts - optional string '-tempids' to return a mapping between any temporary ids in the input transaction data and the actual entity IDs used in the database.  Potentially useful if you're creating objects and then doing stuff with them.

=cut

sub transact {
  my $self = shift;
  my $txn = shift;
  my $opts = { @_ };
    
  $txn = EDN::Literal->new($txn) unless ref($txn);
  my $post = {
	      'tx' => $txn
	     };

  if ($opts->{'-tempids'}) {
    $post->{'tempid-report'} = edn::read('true');
  };
  
  edn_post(
	   "$self->{url}/transact",
	   edn::write($post)
	  );
}

#############################################################################

=head2 

    Title   :   load_ace
    Usage   :   my $results = $db->$load_ace($ace)
    Function:   read in ACE data
    Returns :   results
    Args    :   $ace - string - ACE commands
                $note - string - optional note attached to the transaction

Want to do a multi-part post with the following stuff:
Content-Disposition: form-data; name="format"
ace

Content-Disposition: form-data; name="patch"
Feature : __ALLOCATE__
Public_name "dummy"

=cut

sub load_ace {
  my $self = shift;
  my $ace = shift;
  my $note = shift;
  if (!defined $note) {$note = ''}
    
  my $resp = $self->{client}->post_multipart($self->{url}."/curate/patch", {
									    format => 'ace',
									    patch => $ace,
									    note => $note,
									   });
  die "Failed to connect to nameserver $resp->{'content'}" unless $resp->{'success'};
  return $resp->{'content'};
  
}
#############################################################################

=head2 

    Title   :   get_schema
    Usage   :   my $schema = $db->get_schema;
    Function:   returns a reference to the complete schema of the database (hash-ref)
    Returns :   the complete schema (hash-ref)
    Args    :   

=cut

sub get_schema {
 my ($self) = @_;

 if (!defined $self->{schema}) {

   print "Getting schema ...\n";
   
   my $schema_url = $self->{url}.'/schema';
   my $response = $self->{client}->get($schema_url);
   
   if (!$response->{success}) {
     die "In schema()\nStatus: $response->{status} Reason: $response->{reason}\nContent: $response->{'content'}\n";
   }
   
   $self->{schema} = edn::read($response->{'content'});

   if (!exists $self->{schema}{classes} || !exists $self->{schema}{attributes}) {
     die "In schema()\nThe expected data is not in the schema\n";
   }
 }

 return $self->{schema};
}

#############################################################################

=head2 

    Title   :   fetch_object
    Usage   :   my $object = $db->fetch_object($class, $id);
    Function:   gets the complete object data
    Returns :   the object data or undef if the object is not found in the database
    Args    :   string - datomic class name e.g. 'gene'
                string - ID name to fetch e.g. 'WBGene00300002'

=cut

sub fetch_object {             
 my ($self, $class, $id) = @_;
 
 my $object_url = $self->{url}.'/raw2/' . $class . '/' . $id;
 my $response = $self->{client}->get($object_url);

 if (!$response->{success}) {
   if ($response->{content} eq 'Not found') {return undef}
   die "In fetch_object()\nStatus: $response->{status} Reason: $response->{reason}\nContent: $response->{'content'}\n";
 }

 return edn::read($response->{'content'});

}

#############################################################################

=head2 

    Title   :   get_classes
    Usage   :   my $classes = $db->get_classes();
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

   my $schema = $self->get_schema();
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
    Usage   :   my $attributes = $db->get_attributes();
    Function:   returns the complete attributes of the classes
    Returns :   the complete attributes of the classes - array-ref of hashes
    Args    :   

=cut

sub get_attributes {
 my ($self) = @_;

 if (!defined $self->{attributes}) {

   my $schema = $self->get_schema();
  
   my $attributes = $schema->{attributes};
   $self->{attributes} = $attributes
 }

 return $self->{attributes};
}


#############################################################################

=head2 

    Title   :   get_class_attributes
    Usage   :   my $class_desc = $db->get_class_attributes($class);
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
    Usage   :   my $class_desc = $db->get_class_name_in_datomic($class);
    Function:   input an ACE class name and it returns the datomic equivalent of the ACE class name
    Returns :   returns the datomic equivalent of the ACE class name (i.e. with the '/id' suffix)
    Args    :   class - string - ACE class name or datomic class name

=cut

sub get_class_name_in_datomic {
  my ($self, $class) = @_;
  
  my $classes = $self->get_classes();
  if (!exists  $self->{datomic_classes}{$class}) {
    # warn "Can't find datomic name of '$class', so we assume we already have a datomic name\n";
    return $class;
  }
  return $self->{datomic_classes}{$class};
}


#############################################################################

=head2 

    Title   :   get_class_name_in_ace
    Usage   :   my $class_desc = $db->get_class_name_in_ace($class);
    Function:   input an datomic class name and it returns the ACE equivalent of the datomic class name
    Returns :   returns the ACE equivalent of the datomic class name
    Args    :   class - string - class datomic name

=cut

sub get_class_name_in_ace {
  my ($self, $class) = @_;
  
  my $classes = $self->get_classes(); # ensure the ace_classes hash is populated
  if (!exists  $self->{ace_classes}{$class}) {
    warn "Can't find Ace name of $class, assuming we already have an Ace name\n";
    return $class;
  }
  return $self->{ace_classes}{$class};
}

#############################################################################

=head2 

    Title   :   is_class_a_hash
    Usage   :   my $boolean = $db->is_class_a_hash($class);
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

=head2 

    Title   :   output_class_model_as_ace
    Usage   :   my $model = $db->output_class_model_as_ace($class);
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
	if (!$hidden) { # check if this node has things that we wish to display in the ACE display
	  if (defined $name && (!$alternative_field_name)) {$text .= "$name "}
	  if (defined $type) {
	    if ($type eq 'long') {$text .= "Int "} 
	    elsif ($type eq 'double') {$text .= "Float "} 
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
    Usage   :   my $model = $db->model($class);
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
    Usage   :   my $model = $db->parse_class_object($class_attributes, $class_data, $objstr);
    Function:   returns the class model as a parsed object with an ACE model-like tree structure
    Returns :   array of class elements
    Args    :   class_attributes = array of attributes that have an ident that matches this class
                class_desc = hash-ref of class data for this class
                objstr = ref to array of hashes - populated in this method - one hash for each tag-and-value

=cut

sub parse_class_object {
  my($self, $class_attributes, $class_desc, $objstr) = @_;

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


# +++
# +++ want to grab the locatable hash defined in classes like CDS
# +++



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

  return;
}

#############################################################################

=head2 

    Title   :   add_tags_to_tree
    Usage   :   my $object = $db->add_tags_to_tree($objstr, @ace_tags, $ident);
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



1;
