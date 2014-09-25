#!/usr/bin/perl
# loadmodels.pl

use Ace;
use MongoDB;
use strict;
use Benchmark;
use Data::Dumper;

# set up MongoDB
my $client = MongoDB::MongoClient->new;
my $mongo = $client->get_database( 'wormbase' );

# set up collections for each class
my %classes = map { $_ => $mongo->get_collection( $_ ) } qw/Gene RNAi Phenotype Paper/;

# connect to smallace
my $ace = Ace->connect(-host => 'localhost', -port => 23100)
	or die 'Connection error', Ace->error;

my $batch_size = 100;
my $start_time = new Benchmark;

# Begin loading each of the classes specified
foreach my $class ( keys %classes) {
    print "Start loading $class\n";

    my $aceObjs = $ace->fetch_many($class => '*');

    my $mObjs = $classes{$class};
    $mObjs->drop;

    my @mObjs;
    while(my $obj = $aceObjs->next){
        #print Dumper(_getModel($obj, $class));
        push @mObjs, _getModel($obj, $class);

        # insert every $batch_size objects
        if((scalar @mObjs) >= $batch_size){
            $mObjs->batch_insert(\@mObjs);
            @mObjs = ();
        }
    }

    $mObjs->batch_insert(\@mObjs) if @mObjs;
    print "Finished loading $class\n";
}

my $end_time   = new Benchmark;
my $difference = timediff($end_time, $start_time);

print "It took ", timestr($difference), "\n";

# this should all probably be in a config file somewhere
sub _getModel {
    my ($obj, $class) = @_;

    if($class eq 'Paper'){
        return {
            "_id" => _getObjID($obj, 'Paper'),
            "Reference" => {
                "Title" => _getTag($obj, "Title", { unique => 1 }),
                "Journal" => _getTag($obj, "Journal", { unique => 1 }),
                "Volume" => _getTag($obj, "Volume", { unique => 1 }),
                "Page" => _getTag($obj, "Page", { unique => 1 })
            },
            "Author" => _getTag($obj, "Author"),
            "Brief_citation" => _getTag($obj, "Brief_citation", { unique => 1}),
            "Refers_to" => {
                "Gene" => _getTag($obj, "Gene", { class => "Gene" }),
                "RNAi" => _getTag($obj, "RNAi", { class => "RNAi" })
            }
        };
    } elsif ($class eq 'Gene') {
        return {
            "_id" => _getObjID($obj, 'Gene'),
            "Name"  => { "CGC_name" => _getTag($obj, "CGC_name", { unique => 1}),
                         "Sequence_name" => _getTag($obj, "Sequence_name", { unique => 1}),
                         "Public_name"   =>  _getTag($obj, "Public_name", { unique => 1})
                        },
            "RNAi_result" => _getTag($obj, "RNAi_result", { evidence => 1, class => 'RNAi'}),
            "Concise_description" => _getTag($obj, "Concise_description", { evidence => 1}),
            "Reference" => _getTag($obj, "Reference", { class => "Paper" })
        };
    } elsif ($class eq 'RNAi') {
        return {
            "_id" => _getObjID($obj, 'RNAi'),
            "Evidence" => _getHash($obj),
            "Experiment" => {
                "Delivered_by" => _getTag($obj, "Delivered_by", {unique => 1}),
                "Strain" => _getTag($obj, "Strain")
            },
            "Inhibits" => { "Gene" => _getTag($obj, "Gene", {evidence => 1, class => 'Gene'})},
            "Phenotype" => _getTag($obj, "Phenotype", { class => "Phenotype" }),
            "Phenotype_not_observed" => _getTag($obj, "Phenotype_not_observed", { class => "Phenotype" }),
            "Reference" => _getTag($obj, "Reference", { class => "Paper" })
        };
    } elsif ($class eq 'Phenotype') {
        return {
            "_id" => _getObjID($obj, 'Phenotype'),
            "Description" => _getTag($obj, "Description", { unique => 1 }),
            "Name" => _getTag($obj, "Primary_name"),
            "Attribute_of" => { "RNAi" => _getTag($obj, "RNAi", { class => "RNAi" }),
                                "Not_in_RNAi" => _getTag($obj, "Not_in_RNAi", { class => "RNAi" })}
        };
    }

}




# Fetch the content of a specific tag
# args -
#   unique: return single object for the tag. Otherwise, return an array
#   evidence: attach the evidence hash info to each object returned
sub _getTag {
    my ($obj, $tag, $args) = @_;

    return _getObjID($obj->$tag, $args->{class} || $obj->class) if $args->{unique};

    return [ map {
        $args->{evidence} ?
            ({ _getObjID($_, $args->{class}) => _getHash($_)})
            : _getObjID($_, $args->{class})
    } _get($obj, $tag, $args->{evidence}) ];
}

# generate obj id Class~ID
# replace any instance of '.' with ';;' - mongo restriction
sub _getObjID{
    my ($obj, $class) = @_;
    $obj =~ s/\./\;;/g; # no periods allowed in mongo keys
    return $classes{$class} ? "$class~$obj" : "$obj";
}

# Handles the evidence hash
sub _getHash {
    my ($obj) = @_;

    my %hash;
    foreach my $key ($obj->col){
        my @tag = map { _getObjID($_) } $key->col;
        $hash{"$key"} = \@tag;
    }

    return \%hash;
}

# Returns tag content as string array if possible - add speed.
# Prevent segfault on larger objects.
sub _get{
    my ($obj, $tag, $filled) = @_;
    $obj = $obj->fetch;

    # get the first item in the tag
    my $first_item = $tag ? $obj->get($tag, 0) && $obj->get($tag, 0)->right : $obj->right unless $filled;

    if($first_item && $first_item->{'.raw'}){
        # get our current column location
        my $col = $first_item->{'.col'};

        # grep for rows that are objects
        my $curr;
        my @ret = map { (split '\?', @{$_}[$col])[-1] } grep {  $curr = @{$_}[$col-1] if (@{$_}[$col-1]);
                                (@{$_}[$col] && ($curr eq "?tag?$tag?"));
                        } @{$first_item->{'.raw'}};
        return @ret;
    } else {
        if( _get_count($obj, $tag) > 20000) {
            warn "Skipping $tag for $obj, too large -- will cause segfault\n";
        } else {
            return $obj->$tag;
        }
    }
}



#----------------------------------------------------------------------
# Returns count of objects to be returned with the given tag.
# If no tag is given, it counts the amount of objects in the next column.
# Arg[0]   : The AceDB object to interrogate.
# Arg[1]   : The AceDB schema location to count the amount of retrievable objects;
#
sub _get_count{
    my ($obj, $tag) = @_;
    $obj = $obj->fetch;

    # get the first item in the tag
    my $first_item = $tag ? $obj->get($tag, 0) && $obj->get($tag, 0)->right : $obj->right;

    if($first_item->{'.raw'}){
        # get our current column location
        my $col = $first_item->{'.col'};

        # grep for rows that are objects
        my $curr;
        return scalar(  grep {  $curr = @{$_}[$col-1] if (@{$_}[$col-1]);
                                (@{$_}[$col] && ($curr eq "?tag?$tag?"));
                        } @{$first_item->{'.raw'}} );
    } else {
        # try to avoid this, breaks on larger items
        return scalar $obj->get($tag, 0)->col if $obj->get($tag, 0);
    }
}
