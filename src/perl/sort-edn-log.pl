#!/usr/bin/perl

use strict;
use warnings;

use autodie;
use IPC::System::Simple; # This is for autodiing with system calls

use feature qw(say);

use File::Basename;

say $#ARGV;
if ($#ARGV != 0) {
    say "USAGE: perl bin/sort-edn-log.pl <full path to edn.gz file>";
    exit 1;
}

my $edn_path = $ARGV[0];

my $i = rindex($edn_path,"/");
$i++;

my $log_dir = substr($edn_path, 0, $i);
my $input = substr($edn_path, $i);

my $output = $input;
say $output =~ s/\.gz$/.sort.gz/g;

exit if ($input eq 'helper.edn.gz');

my $command = "cd $log_dir; mkdir -p sort-temp; gzip -dc $input | sort -T sort-temp -k1,1 -s | gzip -c > $output; rm $input";

system($command);
