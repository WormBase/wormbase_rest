#!/usr/bin/perl

use HTTP::Tiny;
use URL::Encode qw(url_encode);
use edn;
use Data::Dumper;

use strict;

# /<storage-alias>/<db-name>/<basis-t>/
# A basis-t of '-' means use the "current database" (i.e. the
# most up-to-date view available).
my $endpoint = 'http://localhost:8421/data/ace/smallace/-/';

#
# We're going to retrieve an entity using a lookup-ref.  In EDN,
# the lookup ref looks like '[:gene/id "foo"]'.  Since Perl doesn't
# have a native keyword type, we need to use a slight trick t
# make one.
#
my $geneid = "WBGene00000051";
my $lookup = [edn::read(':gene/id'), $geneid];
my $query = $endpoint . 'entity?e=' . url_encode(edn::write($lookup));

my $http = HTTP::Tiny->new();
my $resp = $http->get($query);

die "Bad response $resp->{'status'}" unless $resp->{'status'} == 200;

my $gene = edn::read($resp->{'content'});
print Dumper($gene);
