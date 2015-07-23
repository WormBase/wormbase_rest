#!/usr/bin/perl -w

use strict;

use edn;
use Getopt::Long;
use HTTP::Tiny;

my $USAGE = <<END;
Usage: $0 <options>
  Resurrect the indicated ID.
Options:
  --id           Identifier to resurrect.
  --cert         Path to certificate file.
  --key          Path to key file.
  --nameserver   Base URI of the name server to contact.
END

my ($id, $cert, $key, $ns);
GetOptions('id:s'         => \$id,
           'cert:s'       => \$cert,
           'key:s'        => \$key,
           'nameserver:s' => \$ns)
    or die $USAGE;

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

my $query = <<END;
  [:find ?id ?status ?version
   :in \$ ?id
   :where [?gene :gene/id ?id]
          [(get-else \$ ?gene :gene/version 1) ?version]
          (or-join [?gene ?status ?version]
            (and [?gene :gene/status ?sh]
                 [?sh :gene.status/status ?se]
                 [?se :db/ident ?status])
            (and (not [?gene :gene/status _])
                 [(ground :missing) ?status]))]
END

my $result = query($ns, $query, $id);
my $count = scalar @{$result};

die "Could not find identifier $id." unless $count > 0;
die "Ambiguous identifier $id." unless $count == 1;

my ($cid, $live, $version) = @{$result->[0]};

die "$id is Still Alive.\n" if "$live" eq ':gene.status.status/live';
print "Current version is $version.\n";

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
            
my $txr = transact($ns, $txn);
if ($txr eq "OK") {
    print "$cid resurrected.\n"
} else {
    print "Resurrection failed: $txr->{'error'}\n";
}
