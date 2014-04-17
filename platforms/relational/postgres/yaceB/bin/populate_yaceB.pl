#!/usr/bin/perl -w

# populate yace B db

# no data in smallace for gene_paper
# no data in smallace for phenotype_description
# 
# see postgresql_autodoc/yaceB.dia for ERD
# data stored in tables like :
# table                         id      col1    col2
# gene				1	wbgene01
# rnai				9	wbrnai09
# gene_j_rnai			7	1	9
# gene_rnai_j_inferredauto	4	7	text1
# gene_rnai_j_inferredauto	5	7	text2
#
# previous version for yace A db was using Tie::IxHash for storing order of all tags, but it makes processing incredibly slow 
# (5 hours instead of 4 minutes for smallace)
# 2014 03 30
#
# previous version didn't update sequences to setval to highest used value for sequences in 'id' primary key columns in all tables.  2014 04 16


use strict;
use diagnostics;
use DBI;
use Encode qw( from_to is_utf8 );
use Time::HiRes qw( time );

my $dbh = DBI->connect ( "dbi:Pg:dbname=yacebdb", "", "") or die "Cannot connect to database!\n"; 
my $result;

my $lastTime = time;
&getTime("start");

my $outfile = 'logfile';
open (OUT, ">$outfile") or die "Cannot open $outfile : $!"; 


my @infiles = <../ace_source/dump_2014-03-13_A.*.ace>;				# use real .ace files
# my @infiles = <ace_source/test*.ace>;				# use test files
# my @infiles = <ace_source/tiny.ace>;				# use tiny test files

my %storeInFile;
# my $flagFromFileOrByLine = 'file';			# if 'file', write to files and copy ; if 'line', do INSERT through script
my $flagFromFileOrByLine = 'line';			# if 'file', write to files and copy ; if 'line', do INSERT through script
my $flagPrintText = 1;

my %eviTags;						# tags that exist in evidence hash
$eviTags{"Paper_evidence"}++;
$eviTags{"Person_evidence"}++;
$eviTags{"Curator_confirmed"}++;
$eviTags{"Inferred_automatically"}++;
$eviTags{"RNAi_evidence"}++;
$eviTags{"Date_last_updated"}++;

# small ace has 0 gene-paper data and 0 phenotype-description data
my %aceToYace;						# class - tag -> yaceTable
$aceToYace{'gene'}{'mainTable'}			= 'gene';
$aceToYace{'gene'}{'CGC_name'}			= 'gene_namecgc';
$aceToYace{'gene'}{'Sequence_name'}		= 'gene_nameseq';
$aceToYace{'gene'}{'Public_name'}		= 'gene_namepub';
$aceToYace{'gene'}{'RNAi_result'}		= 'gene_j_rnai';
$aceToYace{'gene'}{'Concise_description'}	= 'gene_concise';
# $aceToYace{'gene'}{'Reference'}			= 'gene_j_paper';		# no data for this, removing
$aceToYace{'rnai'}{'mainTable'}			= 'rnai';
$aceToYace{'rnai'}{'Delivered_by'}		= 'rnai_deliveredby';
$aceToYace{'rnai'}{'Strain'}			= 'rnai_strain';
$aceToYace{'rnai'}{'Gene'}			= 'gene_j_rnai';
$aceToYace{'rnai'}{'Phenotype'}			= 'rnai_j_phenotype';
$aceToYace{'rnai'}{'Phenotype_not_observed'}	= 'rnai_j_notphenotype';
$aceToYace{'rnai'}{'Reference'}			= 'paper_j_rnai';
$aceToYace{'rnai'}{'Person_evidence'}		= 'rnai_personevidence';
$aceToYace{'phenotype'}{'mainTable'}		= 'phenotype';
# $aceToYace{'phenotype'}{'Description'}		= 'phenotype_description';	# no data for this, removing
$aceToYace{'phenotype'}{'Primary_name'}		= 'phenotype_nameprimary';
$aceToYace{'phenotype'}{'RNAi'}			= 'rnai_j_phenotype';
$aceToYace{'phenotype'}{'Not_in_RNAi'}		= 'rnai_j_notphenotype';
$aceToYace{'paper'}{'mainTable'}		= 'paper';
$aceToYace{'paper'}{'Author'}			= 'paper_j_author';
$aceToYace{'paper'}{'Title'}			= 'paper_title';
$aceToYace{'paper'}{'Journal'}			= 'paper_journal';
$aceToYace{'paper'}{'Volume'}			= 'paper_volume';
$aceToYace{'paper'}{'Page'}			= 'paper_page';
$aceToYace{'paper'}{'Brief_citation'}		= 'paper_briefcitation';
$aceToYace{'paper'}{'Abstract'}			= 'paper_abstract';
# $aceToYace{'paper'}{'Gene'}			= 'gene_j_paper';			# no data for this, removing
$aceToYace{'paper'}{'RNAi'}			= 'paper_j_rnai';
my %tablesFromAcedb;
foreach my $obj (keys %aceToYace) { foreach my $tag (keys %{ $aceToYace{$obj} }) { $tablesFromAcedb{$aceToYace{$obj}{$tag}}++; } }

my %pgtableHasData;

&getTime("end definitions, start reading files");

my @objects;						# paragraphs from .ace files
my %hash;						# store Class -> object name -> tag -> data
foreach my $infile (@infiles) {
  &getTime("read $infile");
  $/ = "";
  open (IN, "<$infile") or die "Cannot open $infile : $!";
  while (my $entry = <IN>) { 
    chomp $entry; chomp $entry; 
    $entry =~ s/ -O "[\w\-\:]*"//g;			# strip timestamps
    push @objects, $entry; }
  close (IN) or die "Cannot close $infile : $!";
  $/ = "\n";
#   print "F $infile \n";
} # foreach my $infile (@infiles)

&getTime("end reading files, start processing objects");

my %objTables;
my %dataTables;
my %getDataTableId;					# for a given dataTable -> col1 -> col2 get table id column value

my %counters;						# count sequence for each object with generated IDs (author objects + non-object other tables)
my %paperAuthorSortUsed;				# for a given paper track the highest (ordering) sort used;
my %paperReference;					# store columns of paper_reference here, by digit from wbpaper object

while (my $object = shift @objects) {
  my $name;
  if ($object =~ m/^LongText/) {			# LongText objects keep reading until ***LongTextEnd***
    ($name) = $object =~ m/LongText : "([^"]*?)"/;
    my $go = 1;						# keep reading objects until ***LongTextEnd***
    my $data = '';
    while ($go > 0) {
      my $line = shift @objects; 
      if ($line) { 
        if ($line eq '***LongTextEnd***') { $go = 0; }
          else { $data .= $line; } }
    } # while ($go > 0)
    ($data) = &filterForPg($data);
    my ($objPgid) = &wbidToInteger($name);
    $paperReference{$objPgid}{abstract}  = $data; 	# store abstract in paper reference hash
  }  
  else {						# non-LongText objects are in one paragraph
    my @lines = split/\n/, $object;
    my $header = shift @lines;
    next unless ($header =~ m/^(\w+) : "([\w\.\: ]+)"/);
    my ($class, $name) = ($1, $2);
    next if ($class eq 'Phenotype_name');		# already in ?Phenotype as XREF
    next if ($class eq 'Gene_name');			# already in ?Gene as XREF
    ($class) = lc($class);				# class for "object" will be postgres table names, make all lowercase
    my ($objPgid) = &wbidToInteger($name);
    $objTables{$class}{$objPgid}{wbid} = $name;		# store wbid in %objTables
    my $pgtable = $aceToYace{$class}{'mainTable'};   $pgtableHasData{$pgtable}{main}++;
    foreach my $line (@lines) {
        # strip out extra tags in front to get at meaninful tag (or subtag)
      if ($line =~ m/^Evidence\t /) {   $line =~ s/^Evidence\t //; }		# some objects map directly to a .ace #Evidence 
      if ($class eq 'rnai') {
        if ($line =~ m/^Experiment\t /)             { $line =~ s/^Experiment\t //;   }
        if ($line =~ m/^Inhibits\t /)               { $line =~ s/^Inhibits\t //;     } }
      if ($class eq 'phenotype') {
        if ($line =~ m/^Name\t /)                   { $line =~ s/^Name\t //;         }
        if ($line =~ m/^Attribute_of\t /)           { $line =~ s/^Attribute_of\t //; } }
      if ($class eq 'paper') {
        if ($line =~ m/^Reference\t /)              { $line =~ s/^Reference\t //;              }
        if ($line =~ m/^Refers_to\t /)              { $line =~ s/^Refers_to\t //;              } }
      if ($class eq 'gene') {
        if ($line =~ m/^Identity\t /)               { $line =~ s/^Identity\t //;               }
        if ($line =~ m/^Name /)                     { $line =~ s/^Name //;                     }
        if ($line =~ m/^Experimental_info\t /)      { $line =~ s/^Experimental_info\t //;      }
        if ($line =~ m/^Structured_description\t /) { $line =~ s/^Structured_description\t //; } }
      unless ( $line =~ m/^(\w+)\s+(.*?)$/) { print "ERR line does not match tag and data in $name : $line\n"; next; }
      my ($tag, $data) = $line =~ m/^(\w+)\s+(.*?)$/;
      next if ( ($tag eq 'Abstract') && ($class eq 'paper') );		# get actual data from LongText, this just points at the object 
      print OUT "TAG $tag\tNAME $name\tDATA $data\tLINE $line\n";

      my $pgtable = '';
      if ($aceToYace{$class}{$tag}) { $pgtable = $aceToYace{$class}{$tag}; $pgtableHasData{$pgtable}{main}++; }
        else { print "UNACCOUNTED class $class TAG $tag TO YACE\n"; }
      ($tag) = lc($tag);						# tags will be part of postgres table names, make all lowercase
      ($data) = &filterForPg($data);					# strip out backslashes and escape singlequotes

      if ( ($class eq 'gene') && ( ($tag eq 'public_name') || ($tag eq 'cgc_name') || ($tag eq 'sequence_name') ) ||
           ($class eq 'phenotype') && ($tag eq 'primary_name') ) {
              $hash{$class}{$name}{$tag}{$data}++; }			# hash class-tags needed to get a default display_name in object table/index

      my $isText = 0;							# text fields need to be enclosed in singlequotes for insert
      my $addToDataTables = 0;						# only some pgtables should write to data tables
      my $columnClass = $class; my $columnTag = $tag;			# column names may differ from tag and class, make copies and change those
      my $eviData = ''; my $eviPgTable = ''; my $eviColumnTag = ''; my $eviIsText = 0;
      if ($columnClass eq 'gene') {
        if ($tag eq 'cgc_name') {                       $columnTag = 'namecgc'; $isText++;                  }
          elsif ($tag eq 'sequence_name') {             $columnTag = 'nameseq'; $isText++;                  }
          elsif ($tag eq 'public_name') {               $columnTag = 'namepub'; $isText++;                  }
          elsif ($tag eq 'reference') {                 $columnTag = 'paper';                               }
          elsif ($tag eq 'concise_description') {       $columnTag = 'concise'; $isText++;                  
            foreach my $eviTag (sort keys %eviTags) {
              if ($data =~ m/($eviTag)\s+(.*)$/) { 				# DateType not bounded by doublequotes
                $eviTag = $1; $eviData = $2;
                $data =~ s/ ($eviTag)\s+(.*)$//; 				# remove evidence from tag's core data
                if ($data    =~ m/^\"/) {    $data =~ s/^\"//; } if ($data    =~ m/\"$/) {    $data =~ s/\"$//; }
                if ($eviData =~ m/^\"/) { $eviData =~ s/^\"//; } if ($eviData =~ m/\"$/) { $eviData =~ s/\"$//; }
                if ($eviTag eq 'Date_last_updated') {
                  $eviPgTable = 'geneconcise_datelastupdated';   $eviColumnTag = 'datelastupdated';    $eviIsText++; } 
                if ($eviTag eq 'Curator_confirmed') {
                  $eviPgTable = 'geneconcise_curatorconfirmed';  $eviColumnTag = 'curatorconfirmed';   $eviIsText++; } 
                if ($eviTag eq 'Person_evidence') {
                  $eviPgTable = 'geneconcise_personevidence';    $eviColumnTag = 'personevidence';     $eviIsText++; } 
                if ($eviTag eq 'Paper_evidence') {
                  $eviPgTable = 'geneconcise_j_paperevidence';   $eviColumnTag = 'paperevidence';                    } } }
          } # elsif ($tag eq 'concise_description')
          elsif ($tag eq 'rnai_result') {               $columnTag = 'rnai';                                
            foreach my $eviTag (sort keys %eviTags) {
              if ($data =~ m/($eviTag)\s+(.*)$/) { 				# DateType not bounded by doublequotes
                $eviTag = $1; $eviData = $2;
                $data =~ s/ ($eviTag)\s+(.*)$//; 				# remove evidence from tag's core data
                if ($data    =~ m/^\"/) {    $data =~ s/^\"//; } if ($data    =~ m/\"$/) {    $data =~ s/\"$//; }
                if ($eviData =~ m/^\"/) { $eviData =~ s/^\"//; } if ($eviData =~ m/\"$/) { $eviData =~ s/\"$//; }
                if ($eviTag eq 'Inferred_automatically') {
                  $eviPgTable = 'genernai_inferredautomatically'; $eviColumnTag = 'inferredautomatically'; $eviIsText++; } } }
          } # elsif ($tag eq 'rnai_result')
        $addToDataTables++;
      }
      elsif ($columnClass eq 'rnai') {
        if ($tag eq 'delivered_by') {                   $columnTag = 'deliveredby'; $isText++;              }
          elsif ($tag eq 'person_evidence') {           $columnTag = 'personevidence'; $isText++;           }
          elsif ($tag eq 'strain') {                    $isText++;                                          }
          elsif ($tag eq 'phenotype_not_observed') {    $columnTag = 'notphenotype';                        }
          elsif ($tag eq 'reference') {                 $columnTag = 'paper';                               }
          elsif ($tag eq 'gene') {
            foreach my $eviTag (sort keys %eviTags) {
              if ($data =~ m/($eviTag)\s+(.*)$/) { 				# DateType not bounded by doublequotes
                $eviTag = $1; $eviData = $2;
                $data =~ s/ ($eviTag)\s+(.*)$//; 				# remove evidence from tag's core data
                if ($data    =~ m/^\"/) {    $data =~ s/^\"//; } if ($data    =~ m/\"$/) {    $data =~ s/\"$//; }
                if ($eviData =~ m/^\"/) { $eviData =~ s/^\"//; } if ($eviData =~ m/\"$/) { $eviData =~ s/\"$//; }
                if ($eviTag eq 'Inferred_automatically') {
                  $eviPgTable = 'genernai_inferredautomatically'; $eviColumnTag = 'inferredautomatically'; $eviIsText++; } } }
          } # elsif ($tag eq 'gene')
        $addToDataTables++;
      }
      elsif ($columnClass eq 'phenotype') {
        if ($tag eq 'primary_name') {                   $columnTag = 'nameprimary'; $isText++;              }
          elsif ($tag eq 'not_in_rnai') {               $columnTag = 'rnai'; $columnClass = 'notphenotype'; }
        $addToDataTables++;
      }
      elsif ($columnClass eq 'paper') {
        if ( $tag eq 'rnai' ) { $addToDataTables++; }
          elsif ( ($tag eq 'title') || ($tag eq 'journal') || ($tag eq 'volume') || ($tag eq 'page') ) {
              # store reference data in a hash and compile tino %dataTables when it's all processed
              # abstract data comes from longtext, so it's not part of this object read, so cannot generate the objTable pgid to match
            $paperReference{$objPgid}{paper} = $objPgid;		# storing it redundantly for any tag that exists (some tags may not exist)
            $paperReference{$objPgid}{$tag}  = $data; }			
          elsif ($tag eq 'author') {					# for Paper-author generated a wbid and set display_name into %objTables
            my $authorPgid      = ++$counters{author};
            my $paperAuthorPgid = ++$counters{paper_j_author};
            my $sortOrder       = ++$paperAuthorSortUsed{$name};	# the author order in connection to the paper
            $dataTables{paper_j_author}{$paperAuthorPgid}{paper}  = $objPgid;
            $dataTables{paper_j_author}{$paperAuthorPgid}{author} = $authorPgid;    
            $dataTables{paper_j_author}{$paperAuthorPgid}{sort}   = $sortOrder;    
            my $wbid = 'WBAuthor' . &pad10Digits($authorPgid);	# arbitrarily pad to 10 digits
            $objTables{author}{$authorPgid}{wbid}         = $wbid;
            $objTables{author}{$authorPgid}{display_name} = $data; }
      }
      if ($addToDataTables) {					# data that have been accounted for in the code and should be added to dataTable
        if ($isText) { $data = "'" . $data . "'"; }		# if it's of type text put singlequotes for postgres
          else { ($data) = &wbidToInteger($data); }		# if it's not text, all ids are just the integer part
        my $count = 0;
        my $col1 = $objPgid; my $col2 = $data;			# always put the leading column in front of hash, so reverse if data is backward from xref
        if ( ($columnClass eq 'rnai') && ($columnTag eq 'gene') ) { 
          ($col1, $col2) = ($col2, $col1); }
        if ($getDataTableId{$pgtable}{$col1}{$col2}) { 				# if obj-tag data already exists, get stored dataTable.id 
            $count = $getDataTableId{$pgtable}{$col1}{$col2}; }
          else { 								# if it's a new connection, generate a dataTable.id and store
            $count = ++$counters{$pgtable};
            $getDataTableId{$pgtable}{$col1}{$col2}  = $count; }		# store mapping of dataTable to col1 to col2 to dataTable.id value
        $dataTables{$pgtable}{$count}{$columnClass} = $objPgid;
        $dataTables{$pgtable}{$count}{$columnTag}   = $data;    
        if ($eviColumnTag && $eviPgTable && $eviData && $data) {
          if ($eviIsText) { $eviData = "'" . $eviData . "'"; }
            else { ($eviData) = &wbidToInteger($eviData); }
          my $eviCount = ++$counters{$eviPgTable};
          $getDataTableId{$eviPgTable}{$col1}{$col2}         = $eviCount;	# store mapping of dataTable to col1 to col2 to dataTable.id value
          $dataTables{$eviPgTable}{$eviCount}{$pgtable}      = $count;
          $dataTables{$eviPgTable}{$eviCount}{$eviColumnTag} = $eviData;     }
      } # if ($addToDataTables)

    } # foreach my $line (@lines)
  }  
} # while (my $object = shift @objects)

&getTime("end processing objects, process paperReference");

foreach my $papPgid (sort {$a<=>$b} keys %paperReference) {
  my $count = ++$counters{paper_reference};
  foreach my $column (sort keys %{ $paperReference{$papPgid} }) {
    my $data = "'" . $paperReference{$papPgid}{$column} . "'";
    $dataTables{paper_reference}{$count}{$column} = $data; } }

&getTime("end process paperReference, start generating object sql");
my @pgcommands;

foreach my $class (sort keys %objTables) {			# generate data for "object" tables
  &getTime("generate sql for $class objects");
  my %classValues;
  foreach my $pgid (sort {$a<=>$b} keys %{ $objTables{$class} }) {
    my $wbid = $objTables{$class}{$pgid}{wbid};
    my $display_name = $wbid;
    if ($class eq 'author') { 
        if ($objTables{$class}{$pgid}{display_name}) { $display_name = $objTables{$class}{$pgid}{display_name}; } }
      elsif ($class eq 'gene') { 	# prioritize public > cgc > sequence > wbgeneid
        if ($hash{$class}{$wbid}{public_name}) {
            ($display_name) = sort keys %{ $hash{$class}{$wbid}{public_name} }; } 	# there should only be one, arbitrarily pick alphabetically
          elsif ($hash{$class}{$wbid}{cgc_name}) {
            ($display_name) = sort keys %{ $hash{$class}{$wbid}{cgc_name} }; } 		# there should only be one, arbitrarily pick alphabetically
          elsif ($hash{$class}{$wbid}{sequence_name}) {
            ($display_name) = sort keys %{ $hash{$class}{$wbid}{sequence_name} }; } } 	# there should only be one, arbitrarily pick alphabetically
      elsif ($class eq 'phenotype') { 
        if ($hash{$class}{$wbid}{primary_name}) {
          ($display_name) = sort keys %{ $hash{$class}{$wbid}{primary_name} }; } }	# there should only be one, arbitrarily pick alphabetically
      elsif ($class eq 'rnai') { 1; }				# later user History_name tag
      elsif ($class eq 'paper') { 1; }					# later see if there is heuristic, or generate from year and authors
    print OUT "Class $class\t$pgid\t$wbid\t$display_name\n";
    my $cols = qq(id, wb${class}_id, display_name);
    my $data = qq($pgid, '$wbid', '$display_name');
    push @{ $classValues{$cols} }, $data;
  } # foreach my $pgid (sort keys %{ $objTables{$class} })
  foreach my $cols (sort keys %classValues) {
    my ($classValues) = join"), (", @{ $classValues{$cols} };
    push @pgcommands, qq(INSERT INTO $class ($cols) VALUES ($classValues););
  } # foreach my $cols (sort keys %classValues)
  my $highest_pgid = (sort {$b<=>$a} keys %{ $objTables{$class} })[0];			# get highest pgid to set sequence for primary key
  if ($highest_pgid) {
    my $pgClassSequence = $class . '_id_seq';						# sequence name is table '_' column (always id) '_seq'
    push @pgcommands, qq(SELECT setval('$pgClassSequence', $highest_pgid, true);); }	# set sequence value for autoincrement
} # foreach my $class (sort keys %objTables)

&getTime("end generating object sql, start generating dataTables sql");
foreach my $pgtable (sort keys %dataTables) {
  &getTime("generate sql for $pgtable dataTable");
  my %pgtableValues;
  foreach my $pgid (sort {$a<=>$b} keys %{ $dataTables{$pgtable} }) {
    my @cols = qw( id ); my @data = ( $pgid );
    foreach my $col (sort keys %{ $dataTables{$pgtable}{$pgid} }) {
      push @cols, $col;
      push @data, $dataTables{$pgtable}{$pgid}{$col};
    } # foreach my $col (sort {$a<=>$b} keys %{ $dataTables{$pgtable}{$pgid} })
    my $cols = join", ", @cols;
    my $data = join", ", @data;
    print OUT "Table $pgtable\tCols $cols\tData $data\n";
    push @{ $pgtableValues{$cols} }, $data;
  } # foreach my $pgid (sort {$a<=>$b} keys %{ $dataTables{$pgtable} })
  foreach my $cols (sort keys %pgtableValues) {
    my ($pgtableValues) = join"), (", @{ $pgtableValues{$cols} };
    push @pgcommands, qq(INSERT INTO $pgtable ($cols) VALUES ($pgtableValues););
  } # foreach my $cols (sort keys %pgtableValues)
  my $highest_pgid = (sort {$b<=>$a} keys %{ $dataTables{$pgtable} })[0];		# get highest pgid to set sequence for primary key
  if ($highest_pgid) {
    my $pgClassSequence = $pgtable . '_id_seq';						# sequence name is table '_' column (always id) '_seq'
    push @pgcommands, qq(SELECT setval('$pgClassSequence', $highest_pgid, true);); }	# set sequence value for autoincrement
} # foreach my $pgtable (sort keys %dataTables)

&getTime("end generating dataTables sql, start executing sql");
foreach my $pgcommand (@pgcommands) {
  print OUT "$pgcommand\n";
  $result = $dbh->do( $pgcommand );
#   unless (defined $result) { print "FAILED $pgcommand E\n"; }
} # foreach my $pgcommand (@pgcommands)

&getTime("end executing sql, start disconnect from database");
$dbh->disconnect or warn "Disconnection error: $DBI::errstr\n";

&getTime("end disconnect from database, start checking aceClassTag has data");
foreach my $pgtable (sort keys %pgtableHasData)    {			# show which possible junction tables have data
  foreach my $et (sort keys %{ $pgtableHasData{$pgtable} })    { print OUT "hasData\t$pgtable\t$et\n"; } }

&getTime("end checking aceClassTag has data, start checking which aceClassTag lack data");
foreach my $pgtable (sort keys %tablesFromAcedb) { 			# show which tables created from acedb model don't have data
  unless ($pgtableHasData{$pgtable}) { print OUT "no data in smallace for $pgtable\n"; } }

&getTime("end checking which aceClassTag lack data, close logfile");
close (OUT) or die "Cannot close $outfile : $!"; 

&getTime("end script");


sub filterForPg {					# some data needs to be escaped for accessing postgres with DBI
  my ($data) = @_;
  if ($data =~ m/\\/) { $data =~ s/\\//g; } 		# remove all backslashes from .ace file (this is wrong, should only get specific ones escaped by acedb
  if ($flagFromFileOrByLine eq 'line') { 		# in line by line case
    if ($data =~ m/\'/) { $data =~ s/\'/''/g; }		# escape ' into '' for postgres commands
  }
  return $data;
} # sub filterForPg

sub pad10Digits {                # take a number and pad to 10 digits
  my $number = shift;
  if ($number =~ m/^0+/) { $number =~ s/^0+//g; }               # strip leading zeros
  if ($number < 10) { $number = '000000000' . $number; }
  elsif ($number < 100) { $number = '00000000' . $number; }
  elsif ($number < 1000) { $number = '0000000' . $number; }
  elsif ($number < 10000) { $number = '000000' . $number; }
  elsif ($number < 100000) { $number = '00000' . $number; }
  elsif ($number < 1000000) { $number = '0000' . $number; }
  elsif ($number < 10000000) { $number = '000' . $number; }
  elsif ($number < 100000000) { $number = '00' . $number; }
  elsif ($number < 1000000000) { $number = '0' . $number; }
  return $number;
} # sub pad10Digits

sub wbidToInteger {
  my $name = shift;
  my ($objPgid) = $name =~ m/(\d+)/; 
  unless ($objPgid) { print "ERR trying to extract number from $name\n"; }
  $objPgid =~ s/^0+//; 
  unless ($objPgid) { $objPgid = 0; }	
  return $objPgid
} # sub wbidToInteger

sub getTime {
  my ($label) = @_;
  my $curTime = time;
  my $diff = $curTime - $lastTime;
  print qq(TIME $label\tnow $curTime\t$diff to previous\n);
  $lastTime = $curTime;
} # sub getTime

__END__
