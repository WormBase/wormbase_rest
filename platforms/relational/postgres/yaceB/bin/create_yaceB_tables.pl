#!/usr/bin/perl -w

# create tables for yacebdb, a normalized representation of acedb's smallace.  assumptions in README.  2014 03 30

# to remove constraints, instead of altering this script to remove PRIMARY and REFERENCES could do :
# psql -e yacebdb < constraintsDropYaceD

use strict;
use diagnostics;
use DBI;

my $dbh = DBI->connect ( "dbi:Pg:dbname=yacebdb", "", "") or die "Cannot connect to database!\n"; 
my $result;

# put postgres users that should have 'all' access to the table.
# my @users_all = ('apache', 'azurebrd', 'cecilia', '"www-data"');
my @users_all = ('apache', 'azurebrd', '"www-data"');

# put postgres users that should have 'select' access to the table.  mainly so they can log on and see the data from a shell, but would probably work if you set the webserver to have select access, it would just give error messages if someone tried to update data.
# my @users_select = ('acedb');
my @users_select = ();

my @objTables = qw( author gene rnai phenotype paper );
foreach my $table (@objTables) {
    my $createTable  = "CREATE TABLE $table (\n";
    $createTable .= "id              serial NOT NULL PRIMARY KEY,\n";
    $createTable .= "wb${table}_id   text   NOT NULL,\n";
    $createTable .= "display_name    text   NOT NULL,\n";
    $createTable .= "curator_id      text,\n";
    $createTable .= "timestamp       timestamp with time zone DEFAULT \"timestamp\"('now'::text) );\n";
    print "$createTable\n\n";
    $result = $dbh->do( "DROP TABLE IF EXISTS ${table} CASCADE;" );
    $result = $dbh->do( $createTable );
    $result = $dbh->do( "REVOKE ALL ON TABLE ${table} FROM PUBLIC; ");
    foreach my $user (@users_all) { 
      $result = $dbh->do( "GRANT ALL ON TABLE ${table} TO $user; "); }
} # foreach my $table (@objTables)

my %dataTables;
$dataTables{"gene_namecgc"}{one}                             = 'gene';
$dataTables{"gene_namecgc"}{two}                             = 'namecgc';
$dataTables{"gene_namecgc"}{type_two}                        = 'text';
$dataTables{"gene_namepub"}{one}                             = 'gene';
$dataTables{"gene_namepub"}{two}                             = 'namepub';
$dataTables{"gene_namepub"}{type_two}                        = 'text';
$dataTables{"gene_nameseq"}{one}                             = 'gene';
$dataTables{"gene_nameseq"}{two}                             = 'nameseq';
$dataTables{"gene_nameseq"}{type_two}                        = 'text';
$dataTables{"gene_j_rnai"}{one}                              = 'gene';
$dataTables{"gene_j_rnai"}{two}                              = 'rnai';
$dataTables{"gene_j_rnai"}{type_two}                         = 'rnai';
$dataTables{"genernai_inferredautomatically"}{one}           = 'gene_j_rnai';
$dataTables{"genernai_inferredautomatically"}{two}           = 'inferredautomatically';
$dataTables{"genernai_inferredautomatically"}{type_two}      = 'text';
$dataTables{"gene_concise"}{one}                             = 'gene';
$dataTables{"gene_concise"}{two}                             = 'concise';
$dataTables{"gene_concise"}{type_two}                        = 'text';
$dataTables{"geneconcise_curatorconfirmed"}{one}             = 'gene_concise';
$dataTables{"geneconcise_curatorconfirmed"}{two}             = 'curatorconfirmed';
$dataTables{"geneconcise_curatorconfirmed"}{type_two}        = 'text';
$dataTables{"geneconcise_datelastupdated"}{one}              = 'gene_concise';
$dataTables{"geneconcise_datelastupdated"}{two}              = 'datelastupdated';
$dataTables{"geneconcise_datelastupdated"}{type_two}         = 'text';
$dataTables{"geneconcise_j_paperevidence"}{one}              = 'gene_concise';
$dataTables{"geneconcise_j_paperevidence"}{two}              = 'paperevidence';
$dataTables{"geneconcise_j_paperevidence"}{type_two}         = 'paper';
$dataTables{"geneconcise_personevidence"}{one}               = 'gene_concise';
$dataTables{"geneconcise_personevidence"}{two}               = 'personevidence';
$dataTables{"geneconcise_personevidence"}{type_two}          = 'text';
$dataTables{"paper_j_rnai"}{one}                             = 'paper';
$dataTables{"paper_j_rnai"}{two}                             = 'rnai';
$dataTables{"paper_j_rnai"}{type_two}                        = 'rnai';
$dataTables{"phenotype_nameprimary"}{one}                    = 'phenotype';
$dataTables{"phenotype_nameprimary"}{two}                    = 'nameprimary';
$dataTables{"phenotype_nameprimary"}{type_two}               = 'text';
$dataTables{"rnai_j_phenotype"}{one}                         = 'rnai';
$dataTables{"rnai_j_phenotype"}{two}                         = 'phenotype';
$dataTables{"rnai_j_phenotype"}{type_two}                    = 'phenotype';
$dataTables{"rnai_j_notphenotype"}{one}                      = 'rnai';
$dataTables{"rnai_j_notphenotype"}{two}                      = 'notphenotype';
$dataTables{"rnai_j_notphenotype"}{type_two}                 = 'phenotype';
$dataTables{"rnai_strain"}{one}                              = 'rnai';
$dataTables{"rnai_strain"}{two}                              = 'strain';
$dataTables{"rnai_strain"}{type_two}                         = 'text';
$dataTables{"rnai_deliveredby"}{one}                         = 'rnai';
$dataTables{"rnai_deliveredby"}{two}                         = 'deliveredby';
$dataTables{"rnai_deliveredby"}{type_two}                    = 'text';
$dataTables{"rnai_personevidence"}{one}                      = 'rnai';
$dataTables{"rnai_personevidence"}{two}                      = 'personevidence';
$dataTables{"rnai_personevidence"}{type_two}                 = 'text';

foreach my $table (sort keys %dataTables) {
    my $one     = $dataTables{$table}{one};
    my $c1_type = "integer NOT NULL REFERENCES ${one} (id)"; 
    my $two     = $dataTables{$table}{two};
    my $c2_type = "integer NOT NULL REFERENCES $dataTables{$table}{type_two} (id)";
    if ($dataTables{$table}{type_two} eq 'text') { $c2_type = 'text   NOT NULL'; }
    my $createTable  = "CREATE TABLE $table (\n";
    $createTable .= "id              serial NOT NULL PRIMARY KEY,\n";
    $createTable .= "$one            $c1_type,\n";
    $createTable .= "$two            $c2_type,\n";
    $createTable .= "curator_id      text,\n";
    $createTable .= "timestamp       timestamp with time zone DEFAULT \"timestamp\"('now'::text) );\n";
    print "$createTable\n\n";
    $result = $dbh->do( "DROP TABLE IF EXISTS ${table} CASCADE;" );
    $result = $dbh->do( $createTable );
    $result = $dbh->do( "REVOKE ALL ON TABLE ${table} FROM PUBLIC; ");
    foreach my $user (@users_all) { 
      $result = $dbh->do( "GRANT ALL ON TABLE ${table} TO $user; "); }
} # foreach my $table (sort keys %dataTables)

my %orderedDataTables;
$orderedDataTables{"paper_j_author"}{one}                           = 'paper';
$orderedDataTables{"paper_j_author"}{two}                           = 'author';
$orderedDataTables{"paper_j_author"}{type_two}                      = 'author';
foreach my $table (sort keys %orderedDataTables) {
    my $one     = $orderedDataTables{$table}{one};
    my $c1_type = "integer NOT NULL REFERENCES ${one} (id)"; 
    my $two     = $orderedDataTables{$table}{two};
    my $c2_type = "integer NOT NULL REFERENCES $orderedDataTables{$table}{type_two} (id)";
    if ($orderedDataTables{$table}{type_two} eq 'text') { $c2_type = 'text   NOT NULL'; }

    my $createTable  = "CREATE TABLE $table (\n";
    $createTable .= "id              serial NOT NULL PRIMARY KEY,\n";
    $createTable .= "$one            $c1_type,\n";
    $createTable .= "$two            $c2_type,\n";
    $createTable .= "sort            integer NOT NULL,\n";
    $createTable .= "curator_id      text,\n";
    $createTable .= "timestamp       timestamp with time zone DEFAULT \"timestamp\"('now'::text) );\n";
    print "$createTable\n\n";
    $result = $dbh->do( "DROP TABLE IF EXISTS ${table} CASCADE;" );
    $result = $dbh->do( $createTable );
    $result = $dbh->do( "REVOKE ALL ON TABLE ${table} FROM PUBLIC; ");
    foreach my $user (@users_all) { 
      $result = $dbh->do( "GRANT ALL ON TABLE ${table} TO $user; "); }
} # foreach my $table (sort keys %orderedDataTables)

my %paperReferenceTable; 
$paperReferenceTable{"paper_reference"}++;
foreach my $table (sort keys %paperReferenceTable) {
    my $createTable  = "CREATE TABLE $table (\n";
    $createTable .= "id              serial NOT NULL PRIMARY KEY,\n";
    $createTable .= "paper           integer NOT NULL REFERENCES paper (id),\n";
    $createTable .= "title           text,\n";
    $createTable .= "journal         text,\n";
    $createTable .= "volume          text,\n";
    $createTable .= "page            text,\n";
    $createTable .= "abstract        text,\n";
    $createTable .= "curator_id      text,\n";
    $createTable .= "timestamp       timestamp with time zone DEFAULT \"timestamp\"('now'::text) );\n";
    print "$createTable\n\n";
    $result = $dbh->do( "DROP TABLE IF EXISTS ${table} CASCADE;" );
    $result = $dbh->do( $createTable );
    $result = $dbh->do( "REVOKE ALL ON TABLE ${table} FROM PUBLIC; ");
    foreach my $user (@users_all) { 
      $result = $dbh->do( "GRANT ALL ON TABLE ${table} TO $user; "); }
} # foreach my $table (sort keys %paperReferenceTable)

__END__
