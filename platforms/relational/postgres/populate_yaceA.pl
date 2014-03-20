#!/usr/bin/perl -w

# populate yace A db

use strict;
use diagnostics;
use DBI;
use Encode qw( from_to is_utf8 );
use Tie::IxHash;

my $dbh = DBI->connect ( "dbi:Pg:dbname=yaceadb", "", "") or die "Cannot connect to database!\n"; 
my $result = $dbh->do( "ALTER SEQUENCE evi_sequence RESTART WITH 1;" );		# reset sequence to be as if new

my %storeInFile;
my $flagFromFileOrByLine = 'file';			# if 'file', write to files and copy ; if 'line', do INSERT through script
my $flagPrintText = 1;

my %eviTags;						# tags that exist in evidence hash
$eviTags{"Paper_evidence"}++;
$eviTags{"Person_evidence"}++;
$eviTags{"Curator_confirmed"}++;
$eviTags{"Inferred_automatically"}++;
$eviTags{"RNAi_evidence"}++;
$eviTags{"Date_last_updated"}++;

my @infiles = <ace_source/dump*.ace>;				# use real .ace files
# my @infiles = <ace_source/test*.ace>;				# use test files
my @objects;						# paragraphs from .ace files
my %hash;						# store Class -> object name -> tag -> data
foreach my $infile (@infiles) {
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
    unless ($hash{Paper}{$name}{abstract}{$data}) {
      tie %{ $hash{Paper}{$name}{abstract}{$data} }, "Tie::IxHash"; }
  }  
  else {						# non-LongText objects are in one paragraph
    my @lines = split/\n/, $object;
    my $header = shift @lines;
    next unless ($header =~ m/^(\w+) : "([\w\.\: ]+)"/);
    my ($class, $name) = ($1, $2);
    next if ($class eq 'Phenotype_name');		# already in ?Phenotype as XREF
    next if ($class eq 'Gene_name');			# already in ?Gene as XREF
    foreach my $line (@lines) {
        # strip out extra tags in front to get at meaninful tag (or subtag)
      if ($class eq 'RNAi') {
        if ($line =~ m/^Experiment\t /) { $line =~ s/^Experiment\t //; }
        if ($line =~ m/^Inhibits\t /) { $line =~ s/^Inhibits\t //; } }
      if ($class eq 'Phenotype') {
        if ($line =~ m/^Name\t /) { $line =~ s/^Name\t //; }
        if ($line =~ m/^Attribute_of\t /) { $line =~ s/^Attribute_of\t //; } }
      if ($class eq 'Paper') {
        if ($line =~ m/^Reference\t /) { $line =~ s/^Reference\t //; }
        if ($line =~ m/^Refers_to\t /) { $line =~ s/^Refers_to\t //; } }
      if ($class eq 'Gene') {
        if ($line =~ m/^Identity\t /) { $line =~ s/^Identity\t //; }
        if ($line =~ m/^Name /) { $line =~ s/^Name //; }
        if ($line =~ m/^Experimental_info\t /) { $line =~ s/^Experimental_info\t //; }
        if ($line =~ m/^Structured_description\t /) { $line =~ s/^Structured_description\t //; } }
      unless ( $line =~ m/^(\w+)\s+(.*?)$/) { print "ERR line does not match tag and data in $name : $line\n"; next; }
      my ($tag, $data) = $line =~ m/^(\w+)\s+(.*?)$/;
      next if ( ($tag eq 'Abstract') && ($class eq 'Paper') );		# get actual data from LongText, this just points at the object 
	# data that maps directly to a .ace #Evidence maps to a table whose data is the name of the object (like ?Paper has Abstract that points at LongText)
      if ($tag eq 'Evidence') { $data = qq($name $data); }
      ($tag) = lc($tag);						# tags will be part of postgres table names, make all lowercase
      ($data) = &filterForPg($data);					# strip out backslashes and escape singlequotes
      foreach my $eviTag (sort keys %eviTags) {
        if ($data =~ m/($eviTag)\s+(.*)$/) { 				# DateType not bounded by doublequotes
          my $et = $1; my $ed = $2;
          $data =~ s/ ($eviTag)\s+(.*)$//; 				# remove evidence from tag's core data
          if ($data =~ m/^\"/) { $data =~ s/^\"//; } if ($data =~ m/\"$/) { $data =~ s/\"$//; }	# remove leading and trailing doublequotes
          unless ($hash{$class}{$name}{$tag}) { tie %{ $hash{$class}{$name}{$tag} }, "Tie::IxHash"; }	# add to hash and tie to keep sort order
          ($et) = lc($et);						# tags will be part of postgres table names, make all lowercase
          $hash{$class}{$name}{$tag}{$data}{$et}{$ed}++;		# add to hash with evidence tags and data
        }
      } # foreach my $eviTag (sort keys %eviTags)
      my $order = 0;
      unless ($hash{$class}{$name}{$tag}) {
        tie %{ $hash{$class}{$name}{$tag} }, "Tie::IxHash"; }		# add to hash and tie to keep sort order
      if ($data =~ m/^\"/) { $data =~ s/^\"//; } if ($data =~ m/\"$/) { $data =~ s/\"$//; }
      unless ($hash{$class}{$name}{$tag}{$data}) {
        tie %{ $hash{$class}{$name}{$tag}{$data} }, "Tie::IxHash"; }	# add to hash by using tie, sort order not meaningful here
    } # foreach my $line (@lines)
  }  
} # while (my $object = shift @objects)

my @pgcommands;
my %classToDatatype; 							# each .ace Class maps to a 3-letter datatype for postgres
&populateClassToDatatype();


foreach my $class (sort keys %hash) {
  foreach my $name (sort keys %{ $hash{$class} }) {
    if ($flagPrintText) { print "Class $class\tName $name\n"; }
    my $datatype = $classToDatatype{$class};
    foreach my $tag (sort keys %{ $hash{$class}{$name} }) {
      my $pgTable = $datatype . '_' . $tag;
      my $order = 0;
      foreach my $data (keys %{ $hash{$class}{$name}{$tag} }) {
        $order++; my $eviId = 'NULL';
        if ($flagFromFileOrByLine eq 'file') { $eviId = '\N'; }		# in dump file it should be \N instead of NULL
        if (scalar keys %{ $hash{$class}{$name}{$tag}{$data} } > 0) { 	# if there is evidence data, get a new evidence ID
          ($eviId) = &getNextEviId(); }
        if ($flagPrintText) { print "TAG $tag\tORDER $order\tDATA $data\tEVIID $eviId\n"; }
        if ($flagFromFileOrByLine eq 'file') { push @{ $storeInFile{$pgTable} }, qq($name\t$order\t$data\t$eviId); }
          elsif ($flagFromFileOrByLine eq 'line') { push @pgcommands, "INSERT INTO $pgTable VALUES('$name', $order, E'$data', $eviId);"; }
        foreach my $eviTag (sort keys %{ $hash{$class}{$name}{$tag}{$data} }) {
          foreach my $eviData (sort keys %{ $hash{$class}{$name}{$tag}{$data}{$eviTag} }) {
            if ($eviData =~ m/^\"/) { $eviData =~ s/^\"//; } if ($eviData =~ m/\"$/) { $eviData =~ s/\"$//; }
            if ($flagPrintText) { print "TAG $tag\tORDER $order\tDATA $data\tEVI $eviTag\tED $eviData\n"; }
            my $pgEviTable = 'evi_' . $eviTag;
            if ($flagFromFileOrByLine eq 'file') { push @{ $storeInFile{$pgEviTable} }, qq($eviId\t1\t$eviData\t\\N); }
              elsif ($flagFromFileOrByLine eq 'line') { push @pgcommands, "INSERT INTO $pgEviTable VALUES('$eviId', 1, E'$eviData', NULL);"; }
          } # foreach my $eviData (sort keys %{ $hash{$class}{$name}{$tag}{$data}{$eviTag} })
        } # foreach my $eviTag (sort keys %{ $hash{$class}{$name}{$tag}{$data} })
      } # foreach my $data (keys %{ $hash{$class}{$name}{$tag} })
    } # foreach my $tag (sort keys %{ $hash{$class}{$name} })
    if ($flagPrintText) { print "\n"; }
  } # foreach my $name (sort keys %{ $hash{$class} })
} # foreach my $class (sort keys %hash)

if ($flagFromFileOrByLine eq 'file') {					# if doing a copy by dump file, write to files and copy
  my $pgFilesDir = '/home/azurebrd/work/acedb/yace/table_dumps/';
  foreach my $table (sort keys %storeInFile) {
    my $pgfile = $pgFilesDir . $table . '.pg';
    open (PG, ">$pgfile") or die "Cannot open $pgfile : $!";
    foreach my $line (@{ $storeInFile{$table} }) { print PG "$line\n"; }
    close (PG) or die "Cannot close $pgfile : $!";
    push @pgcommands, qq(COPY $table FROM '$pgfile';);
  } # foreach my $table (sort keys %storeInFile)
} # if ($flagFromFileOrByLine eq 'file')

foreach my $pgcommand (@pgcommands) {
  print "$pgcommand\n";
  $result = $dbh->do( $pgcommand );
#   unless (defined $result) { print "FAILED $pgcommand E\n"; }
} # foreach my $pgcommand (@pgcommands)

sub filterForPg {
  my ($data) = @_;
  if ($data =~ m/\\/) { $data =~ s/\\//g; } 		# remove all backslashes from .ace file (this is wrong, should only get specific ones escaped by acedb
  if ($flagFromFileOrByLine eq 'line') { 		# in line by line case
    if ($data =~ m/\'/) { $data =~ s/\'/''/g; }		# escape ' into '' for postgres commands
  }
  return $data;
} # sub filterForPg

sub populateClassToDatatype {
  $classToDatatype{'Gene'}	= 'gin';
  $classToDatatype{'RNAi'}	= 'rna';
  $classToDatatype{'Phenotype'}	= 'phe';
  $classToDatatype{'Paper'}	= 'pap';
} # sub populateClassToDatatype

sub getNextEviId {
#   return 1;						# uncomment to test without postgres updating sequence
  $result = $dbh->prepare( "SELECT nextval('evi_sequence')" );
  $result->execute() or die "Cannot prepare statement: $DBI::errstr\n"; 
  my @row = $result->fetchrow(); 
  return $row[0]; 
} # sub getNextEviId


__END__

