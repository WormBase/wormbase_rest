#!/usr/bin/perl
# tests.pl

use MongoDB;
use strict;

# set up MongoDB
my $client = MongoDB::MongoClient->new;
my $mongo = $client->get_database( 'test' );

# set up collections for each class
my %classes = map { $_ => $mongo->get_collection( $_ ) } qw/Gene RNAi Phenotype Paper/;

# tests

my $geneCursor = $classes{'Gene'}->find();
while(my $doc = $geneCursor->next) {
    print $doc->{'_id'} . ': ' . join(', ', keys %{$doc->{'Concise_description'}[0]}) . ' - ' . scalar @{$doc->{'RNAi_result'}} . ': ' . $doc->{'Name'}{'Public_name'} . "\n";
}


# my $rnaiCursor = $mRnai->find();
# while(my $doc = $rnaiCursor->next) {
#     print $doc->{'_id'} . ': ' . join(', ', keys %{$doc->{'Inhibits'}{'Gene'}[0]}) . ' - ' . scalar @{$doc->{'Inhibits'}{'Gene'}} . ': ' . $doc->{'Phenotype'}[0] . "\n";
# }


# my $phenCursor = $classes{'Phenotype'}->find();
# while(my $doc = $phenCursor->next) {
#     print $doc->{'_id'} . ': ' . $doc->{'Attribute_of'}{'RNAi'}[0] . ' - ' . scalar @{$doc->{'Attribute_of'}{'RNAi'}} . ' : ' . $doc->{'Description'} . "\n";
# }


# my $paperCursor = $mPapers->find();
# while(my $doc = $paperCursor->next) {
#     print $doc->{'_id'} . ': ' . $doc->{'Refers_to'}{'Gene'}[0] . ' - ' . $doc->{'Brief_citation'} . "\n";
# }


# # print contents of mongo
# foreach my $col (keys %classes){
#     print "$col\n";
#     my $collection = $classes{$col};
#     my $cursor = $collection->find();

#     while(my $doc = $cursor->next) {
#         print "\t" . $doc->{'_id'} . "\n";
#     }
# }

