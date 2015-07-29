#!/usr/bin/perl -w

use strict;

use edn;
use Getopt::Long;
use HTTP::Tiny;
use Data::Dumper;

my $USAGE = <<END;
Usage: $0 <options>
  Create and assign IDs for new features.
Options:
  --request      Number of features to request
  --cert         Path to certificate file.
  --key          Path to key file.
  --nameserver   Base URI of the name server to contact.
END

my ($request, $cert, $key, $ns);
GetOptions('request:i'    => \$request,
           'cert:s'       => \$cert,
           'key:s'        => \$key,
           'nameserver:s' => \$ns)
    or die $USAGE;

die "give me a number of features that you want\n"  unless ($request =~ /^\d+$/);

my $client = HTTP::Tiny->new(
    max_redirect => 0, 
    SSL_options => {
        SSL_cert_file => $cert, 
        SSL_key_file =>  $key
    });

sub edn_post {
    my ($uri, $content) = @_;
    my $resp = $client->post($uri, {
        content => $content,
        headers => {
            'content-type' => 'application/edn'
        }
    });
    die "Failed to connect to nameserver $resp->{'content'}" unless $resp->{'success'};
    return edn::read($resp->{'content'});
}

sub query {
    my $ns = shift;
    my $q = shift;
    my $params = [ {'db/alias' => 'x'}, @_ ];
    
    $q = EDN::Literal->new($q) unless ref($q);

    my $post = {
        'q' => $q
    };
    if (scalar(@$params) > 0) {
        $post->{'args'} = $params;
    };

    edn_post(
        "$ns/api/query", 
        edn::write($post)
    );
}

sub transact {
    my $ns = shift;
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
        "$ns/transact",
        edn::write($post)
    );
}

my $tempids = [];
for my $i (1..$request) {
    push $tempids, edn::read("#db/id [:db.part/user @{[-100500 - $i]}]");
}

my $txn = [[edn::read(':wb/mint-identifier'),
            edn::read(':feature/id'),
            $tempids]];

            
my $txr = transact($ns, $txn);
my $ids = $txr->{'ids'};
print "First ID: $ids->[0]\n";
print "Last ID: $ids->[-1]\n";
