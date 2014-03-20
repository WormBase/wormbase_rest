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
$result = $dbh->prepare( "SELECT joinkey FROM gin_public_name" );
$result->execute() or die "Cannot prepare statement: $DBI::errstr\n"; 
while (my @row = $result->fetchrow) {
  if ($row[0]) { push @wbgenes, $row[0]; } }

my @phenotypes;
$result = $dbh->prepare( "SELECT joinkey FROM phe_primary_name" );
$result->execute() or die "Cannot prepare statement: $DBI::errstr\n"; 
while (my @row = $result->fetchrow) {
  if ($row[0]) { push @phenotypes, $row[0]; } }

my $highestRNAi;
$result = $dbh->prepare( "SELECT joinkey FROM rna_reference ORDER BY joinkey DESC" );
$result->execute() or die "Cannot prepare statement: $DBI::errstr\n"; 
my @row = $result->fetchrow(); 
($highestRNAi) = $row[0] =~ m/(\d+)/;

srand;

my @pgcommands;
my $start = &time();
for my $i (1 .. $amountUpdates) {
  my $arrIndex  = rand @wbgenes;
  my $wbgene    = $wbgenes[$arrIndex];
  $arrIndex     = rand @phenotypes;
  my $phenotype = $phenotypes[$arrIndex];
  $highestRNAi++;
  my $rnai = 'WBRNAi' . &pad8Zeros($highestRNAi);
#   print "$wbgene\t$phenotype\t$rnai\n";
  &addToPg('phe_rnai', $phenotype, $rnai, 0);
  &addToPg('rna_phenotype', $rnai, $phenotype, 0);
  &addToPg('gin_rnai_result', $wbgene, $rnai, 1);
  &addToPg('rna_gene', $rnai, $wbgene, 1);
}
foreach my $pgcommand (@pgcommands) {
#   print "PG $pgcommand\n";
  $dbh->do( $pgcommand );
} # foreach my $pgcommand (@pgcommands)
my $countTableChanges = scalar(@pgcommands);
my $end = &time(); my $diff = $end - $start; print qq(creating $amountUpdates updates with $countTableChanges table changes took $diff seconds\n);

sub addToPg {
  my ($pgtable, $joinkey, $data, $eviFlag) = @_;
  $result = $dbh->prepare( "SELECT joinkey, data FROM $pgtable WHERE joinkey = '$joinkey' AND data = '$data'" );
  $result->execute() or die "Cannot prepare statement: $DBI::errstr\n"; 
  my @row = $result->fetchrow();
  if ($row[0]) { return; }				# already exists, don't do anything
  my $order = 1;
  $result = $dbh->prepare( "SELECT sort FROM $pgtable WHERE joinkey = '$joinkey' ORDER BY sort DESC" );
  $result->execute() or die "Cannot prepare statement: $DBI::errstr\n"; 
  @row = $result->fetchrow();
  if ($row[0]) { $order = $row[0] + 1; }
  my $eviId = 'NULL';
  if ($eviFlag) { 
    ($eviId) = &getNextEviId();
    push @pgcommands, "INSERT INTO evi_inferred_automatically VALUES ('$eviId', 1, 'RNAi_primary', NULL);"; }
  push @pgcommands, "INSERT INTO $pgtable VALUES ('$joinkey', $order, '$data', $eviId);";
} # sub addToPg

# phenotype to rnai	phe_rnai
# rnai to phenotype	rna_phenotype
# gene to rnai	gin_rnai_result	evi
#   Inferred_automatically	RNAi_primary
# rnai to gene	rna_gene evi
#   Inferred_automatically	RNAi_primary


sub getNextEviId {
#   return 1;                                           # uncomment to test without postgres updating sequence
  $result = $dbh->prepare( "SELECT nextval('evi_sequence')" );
  $result->execute() or die "Cannot prepare statement: $DBI::errstr\n";
  my @row = $result->fetchrow();
  return $row[0];
} # sub getNextEviId


sub pad8Zeros {         # take a number and pad to 8 digits
  my $number = shift;
  if ($number =~ m/^0+/) { $number =~ s/^0+//g; }               # strip leading zeros
  if ($number < 10) { $number = '0000000' . $number; }
  elsif ($number < 100) { $number = '000000' . $number; }
  elsif ($number < 1000) { $number = '00000' . $number; }
  elsif ($number < 10000) { $number = '0000' . $number; }
  elsif ($number < 100000) { $number = '000' . $number; }
  elsif ($number < 1000000) { $number = '00' . $number; }
  elsif ($number < 10000000) { $number = '0' . $number; }
  return $number;
} # sub pad8Zeros
