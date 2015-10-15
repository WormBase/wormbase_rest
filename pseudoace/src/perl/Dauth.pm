#!/software/bin/perl -w
#
# Dauth.pm
# 
# by Gary Williams
#
# Perl interface to Thomas Down's Datomic REST servers & authentication
#
# Last updated by: $Author: gw3 $     
# Last updated on: $Date: 2014-12-05 09:51:03 $      

=pod

=head1 NAME

 Dauth

=head1 SYNOPSIS


  my @db_names = Dauth::get_list_of_databases(); # return a list of known database names

  use Dauth;
  my $db = Dauth->new();         # prompts you to supply a database name and URI and SSL certificate file details which are stored for later
  my $db = Dauth->new($db_name); # to use a specific database

  my $results = $db->$query($datomic_querystr, $queryargs);   # get the results of a datomic query
  my $results = $db->$transact($txn, $opts);                  # do a transaction to change data
  my $results = $db->$load_ace($ace);                         # read ACE data into the database
  my $schema = $db->schema;                                   # returns the complete schema of the database
  my $object = $db->fetch_object($class, $id);                # returns a fully populated data structure for an object

=head1 CONTACT

Gary gw3@ebi.ac.uk

=head1 APPENDIX

 The rest of the documentation details each of the object methods.
 Internal methods are preceded with a _

=cut

package Dauth;

use warnings;
use edn;          # Thomas Down's module for wrapping data in EDN. See:  https://github.com/dasmoth/edn-perl
use HTTP::Tiny;
use HTTP::Tiny::Multipart;
use Config::IniFiles;



=head2

    Title   :   new
    Usage   :   my $db = Dauth->new();
    Function:   initialises the connection to the datomic REST server
    Returns :   Dauth object;
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
  
  Title   :   get_list_of_databases
  Usage   :   @db_names = Dauth::get_list_of_databases();
  Function:   get a lis of the available names of databases stored in the certificates file
  Returns :   array of text database names
  Args    :   none

=cut

sub get_list_of_databases {
  my ($ini, $path) = Dauth::get_db_ini_file();
  return $ini->Sections();
}


#############################################################################
# Stuff for accessing the REST services
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
    Returns :   array of arrays holding the result of the query
    Args    :   q - string - datomic query
                params - string - optional list of arguments for the query (e.g. IDs from a Keyset:  '["WBGene00000016" "WBGene00000017"]')

Example usage:
print Dumper($db->query('[:find ?c :in $ :where  [?c :gene/id "WBGene00018635"]]', ''));

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
                $opts - optional string '-tempids' to return a mapping between any temporary ids in the input transaction data and the actual entity IDs used in the database.  Potentially useful if you are creating objects and then doing stuff with them.

Example usage:
my $cid = "WBGene0001234";
my $txn = [{
    'db/id'        => [edn::read(':gene/id'), $cid],
    'gene/version' => ($version + 1),
    'gene/status'  => {
        'gene.status/status' => edn::read(':gene.status.status/live')
    },
    'gene/version-change' => {
        'gene.version-change/version' => ($version + 1),
        'gene-history-action/resurrected' => edn::read('true')
    }
}];


print edn::write($txn);
            
my $txr = transact($txn);
if ($txr eq "OK") {
    print "$cid resurrected.\n"
} else {
    print "Resurrection failed: $txr->{'error'}\n";
}

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
    Function:   read ACE data into the database
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

Example usage:
my $results = $db->get_schema();
print "Data:". Dumper($results->{classes})."\n"; 
print "Data:". Dumper($results->{attributes})."\n"; 

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

Example usage:
print Dumper($db->fetch_object('gene', 'WBGene00000016'));

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

1;
