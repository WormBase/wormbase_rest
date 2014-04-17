#!/usr/bin/perl -w

# test updating and writing to yace by creating a new rnai object, and connecting it to a random gene and a random phenotype.  
# improvement over yaceA since instead of 6 table inserts per update, doing 4 table inserts with 2 column returns.  2014 04 16
# 
# gene updates :
# script update_yaceB_geneRnaiPhenotype.pl
# creating 10000 updates with 4x table changes took 349.515940904617 seconds
# creating 10000 updates with 4x table changes took 352.190929889679 seconds
# creating 10000 updates with 4x table changes took 349.06324005127 seconds

# - 4 tables, create an entry in 'rnai', make connections to that rnai in 'gene_j_rnai' and 'rnai_j_phenotype', then from 'gene_j_rnai' to 'genernai_inferredautomatically'
# process :
# - get all genes with public_name
# - get all phenotype with primary_name
# - get highest existing WBRNAi ID.
# - 10000 times
# - - generate a new WBRNAi ID (get value from id column)
# - - randomly choose a WBGene and WBPhenotype
# - - assign rnai-phenotype mapping to table 'rnai_j_phenotype'
# - - assign gene-rnai mapping to table 'gene_j_rnai' (get value from id column)
# - - assign gene-rnai inferredautomatically mapping to table 'genernai_inferredautomatically' 


use strict;
use diagnostics;
use DBI;
use Encode qw( from_to is_utf8 );
use Time::HiRes qw(time);


my $dbh = DBI->connect ( "dbi:Pg:dbname=yacebdb", "", "") or die "Cannot connect to database!\n"; 
my $result;

my $amountUpdates = 10000;

my @wbgenes;
$result = $dbh->prepare( "SELECT gene FROM gene_namepub" );
$result->execute() or die "Cannot prepare statement: $DBI::errstr\n"; 
while (my @row = $result->fetchrow) {
  if ($row[0]) { push @wbgenes, $row[0]; } }

my @phenotypes;
$result = $dbh->prepare( "SELECT phenotype FROM phenotype_nameprimary;" );
$result->execute() or die "Cannot prepare statement: $DBI::errstr\n"; 
while (my @row = $result->fetchrow) {
  if ($row[0]) { push @phenotypes, $row[0]; } }

my $highestRNAi;			# highest id value from id column of rnai table
$result = $dbh->prepare( "SELECT id FROM rnai ORDER BY id DESC LIMIT 1;" );
$result->execute() or die "Cannot prepare statement: $DBI::errstr\n"; 
my @row = $result->fetchrow(); 
($highestRNAi) = $row[0] =~ m/(\d+)/;

srand;

my $curator_id   = 'some curator';
my $gjr_inf_auto = 'RNAi_primary';
my $start = &time();
for my $i (1 .. $amountUpdates) {
  my $arrIndex   = rand @wbgenes;
  my $gene       = $wbgenes[$arrIndex];
  $arrIndex      = rand @phenotypes;
  my $phenotype  = $phenotypes[$arrIndex];
  $highestRNAi++;
  my $wbrnai = 'WBRNAi' . &pad8Zeros($highestRNAi);
  $result = $dbh->prepare( "INSERT INTO rnai (wbrnai_id, display_name, curator_id) VALUES ('$wbrnai', '$wbrnai', '$curator_id') RETURNING id;" );
  $result->execute() or die "Cannot prepare statement: $DBI::errstr\n"; 
  my @row = $result->fetchrow();
  my $rnai = $row[0];
  $result = $dbh->do( "INSERT INTO rnai_j_phenotype (rnai, phenotype, curator_id) VALUES ('$rnai', '$phenotype', '$curator_id');" );
  $result = $dbh->prepare( "INSERT INTO gene_j_rnai      (gene, rnai, curator_id) VALUES ('$gene', '$rnai', '$curator_id') RETURNING id;" );
  $result->execute() or die "Cannot prepare statement: $DBI::errstr\n"; 
  @row = $result->fetchrow();
  my $gene_j_rnai  = $row[0];
  $result = $dbh->do( "INSERT INTO genernai_inferredautomatically (gene_j_rnai, inferredautomatically, curator_id) VALUES ('$gene_j_rnai', '$gjr_inf_auto', '$curator_id');" );
#   print "created $rnai for $wbrnai linked to gene $gene and phenotype $phenotype with gene-rnai evidence $gjr_inf_auto\n";
}
my $end = &time(); my $diff = $end - $start; print qq(creating $amountUpdates updates with 4x table changes took $diff seconds\n);



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
