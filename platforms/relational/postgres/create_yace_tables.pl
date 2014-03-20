#!/usr/bin/perl -w

# create <datatype>_<table> table, <datatype>_<table>_idx index, for yacedb tables.  all tables have joinkey, order, data, evidence.  2014 03 15


use strict;
use diagnostics;
use DBI;

my $dbh = DBI->connect ( "dbi:Pg:dbname=yaceadb", "", "") or die "Cannot connect to database!\n"; 
my $result;

# put postgres users that should have 'all' access to the table.
my @users_all = ('apache', 'azurebrd', 'cecilia', '"www-data"');

# put postgres users that should have 'select' access to the table.  mainly so they can log on and see the data from a shell, but would probably work if you set the webserver to have select access, it would just give error messages if someone tried to update data.
my @users_select = ('acedb');

my %tables;
$tables{gin}{cgc_name}			= 'UNIQUE';
$tables{gin}{sequence_name}		= 'UNIQUE';
$tables{gin}{public_name}		= 'UNIQUE';
$tables{gin}{rnai_result}		= 'normal';
$tables{gin}{concise_description}	= 'normal';
$tables{gin}{reference}			= 'normal';

$tables{rna}{evidence}			= 'normal';
$tables{rna}{delivered_by}		= 'UNIQUE';
$tables{rna}{strain}			= 'normal';
$tables{rna}{gene}			= 'normal';
$tables{rna}{phenotype}			= 'normal';
$tables{rna}{phenotype_not_observed}	= 'normal';
$tables{rna}{reference}			= 'normal';

$tables{phe}{description}		= 'UNIQUE';
$tables{phe}{primary_name}		= 'UNIQUE';
$tables{phe}{rnai}			= 'normal';
$tables{phe}{not_in_rnai}		= 'normal';

$tables{pap}{author}			= 'normal';
$tables{pap}{title}			= 'UNIQUE';
$tables{pap}{journal}			= 'UNIQUE';
$tables{pap}{volume}			= 'UNIQUE';
$tables{pap}{page}			= 'UNIQUE';
$tables{pap}{brief_citation}		= 'UNIQUE';
$tables{pap}{abstract}			= 'normal';
$tables{pap}{gene}			= 'normal';
$tables{pap}{rnai}			= 'normal';

$tables{evi}{paper_evidence}		= 'normal';
$tables{evi}{person_evidence}		= 'normal';
$tables{evi}{curator_confirmed}		= 'normal';
$tables{evi}{inferred_automatically}	= 'normal';
$tables{evi}{rnai_evidence}		= 'normal';
$tables{evi}{date_last_updated}		= 'UNIQUE';

foreach my $datatype (sort keys %tables) {
  foreach my $table (sort keys %{ $tables{$datatype} }) {
    my $indexType = $tables{$datatype}{$table};						# some tables are unique per object
    my $joinkeyType = 'text'; if ($datatype eq 'evi') { $joinkeyType = 'integer'; }	# most tables use text, but evidence table is integer
    $result = $dbh->do( "DROP TABLE ${datatype}_${table};" );
    $result = $dbh->do( "CREATE TABLE ${datatype}_${table} (
                           joinkey $joinkeyType, 
                           sort integer,
                           data text,
                           evidence integer ); " );
    $result = $dbh->do( "REVOKE ALL ON TABLE ${datatype}_${table} FROM PUBLIC; ");
    foreach my $user (@users_select) { 
      $result = $dbh->do( "GRANT SELECT ON TABLE ${datatype}_${table} TO $user; "); }
    foreach my $user (@users_all) { 
      $result = $dbh->do( "GRANT ALL ON TABLE ${datatype}_${table} TO $user; "); }
    if ($indexType eq 'normal') { $indexType = ''; }
    $result = $dbh->do( "CREATE $indexType INDEX ${datatype}_${table}_idx ON ${datatype}_${table} USING btree (joinkey); ");
  } # foreach my $table (sort keys %{ $tables{$datatype} })
} # foreach my $datatype (sort keys %tables)

my %sequences;
$sequences{evi}++;
foreach my $datatype (sort keys %sequences) {
    $result = $dbh->do( "DROP SEQUENCE ${datatype}_sequence;" );
    $result = $dbh->do( "CREATE SEQUENCE ${datatype}_sequence
                           START WITH 1
                           INCREMENT BY 1
                           NO MAXVALUE
                           NO MINVALUE
                           CACHE 1; " );
    $result = $dbh->do( "REVOKE ALL ON SEQUENCE ${datatype}_sequence FROM PUBLIC; ");
    foreach my $user (@users_select) { 
      $result = $dbh->do( "GRANT SELECT ON SEQUENCE ${datatype}_sequence TO $user; "); }
    foreach my $user (@users_all) { 
      $result = $dbh->do( "GRANT ALL ON SEQUENCE ${datatype}_sequence TO $user; "); }
} # foreach my $datatype (sort keys %sequences)



__END__

#     $result = $dbh->do( "CREATE TABLE ${datatype}_${table} (
#                            joinkey $joinkeyType, 
#                            ${datatype}_sort integer,
#                            ${datatype}_data text,
#                            ${datatype}_evidence integer,
#                            ${datatype}_timestamp timestamp with time zone DEFAULT \"timestamp\"('now'::text)); " );
