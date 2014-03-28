#!/usr/bin/perl -w

# test updating and writing to yace  2014 03 16

use strict;
use diagnostics;
use DBI;
use Encode qw( from_to is_utf8 );
use Time::HiRes qw(time);


my $dbh = DBI->connect ( "dbi:Pg:dbname=yaceadb", "", "") or die "Cannot connect to database!\n"; 
my $result;

my $amountUpdates = 10000;

my @wbgenes;
$result = $dbh->prepare( "SELECT joinkey FROM gin_concise_description" );
$result->execute() or die "Cannot prepare statement: $DBI::errstr\n"; 
while (my @row = $result->fetchrow) {
  if ($row[0]) { push @wbgenes, $row[0]; } }

srand;

my $start = &time();
for my $i (1 .. $amountUpdates) {
  my $arrIndex = rand @wbgenes;
  my $wbgene    = $wbgenes[$arrIndex];
  $result = $dbh->do( "UPDATE gin_concise_description SET data = 'iteration $i' WHERE joinkey = '$wbgene';" );
}
my $end = &time(); my $diff = $end - $start; print qq(updating $amountUpdates times took $diff seconds\n);


