#!/usr/bin/perl -w

# test updating and writing to yace.  update a concise description to a gene, only changing the curator/timestamp associated with that, not the #Evidence (assuming it's the same curator fixing a typo or some such)  2014 04 16 
 
# yaceB times :
# updating 10000 times took 85.923614025116 seconds
# updating 10000 times took 85.8680741786957 seconds
# updating 10000 times took 86.1078090667725 seconds



use strict;
use diagnostics;
use DBI;
use Encode qw( from_to is_utf8 );
use Time::HiRes qw(time);


my $dbh = DBI->connect ( "dbi:Pg:dbname=yacebdb", "", "") or die "Cannot connect to database!\n"; 
my $result;

my $amountUpdates = 10000;

my @genes;
$result = $dbh->prepare( "SELECT gene FROM gene_concise" );		# only update genes that have concise_description
$result->execute() or die "Cannot prepare statement: $DBI::errstr\n"; 
while (my @row = $result->fetchrow) {
  if ($row[0]) { push @genes, $row[0]; } }

srand;

my $curator_id = 'some_curator';

my $start = &time();
for my $i (1 .. $amountUpdates) {
  my $arrIndex = rand @genes;
  my $geneid   = $genes[$arrIndex];
  $result = $dbh->do( "UPDATE gene_concise SET concise = 'iteration $i', curator_id = '$curator_id', timestamp = CURRENT_TIMESTAMP WHERE gene = '$geneid';" );
}
my $end = &time(); my $diff = $end - $start; print qq(updating $amountUpdates times took $diff seconds\n);


