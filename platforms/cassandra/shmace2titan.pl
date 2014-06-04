#!/usr/bin/env perl
# convert ShmACE to GraphSON (Titan Version not Faunus one)
# Problems:
#  a.) unique ids for each vertex
#  b.) unique ids for each edge
#  c.) edges can have properties, but no additional vertex connections ... at least at the moment

use Ace;
use feature qw(say);

my $db = Ace->connect(-path => shift)||die(Ace->error);

# lookups
my %name2vertex;
my $vertC=1;
my $edgeC=1;
my @edges;

say '{"graph": { "mode":"NORMAL","vertices":[';
&print_genes;
say ',';
&print_rnai;
say ',';
&print_phenotype;
say ',';
&print_paper;
say '],"edges":[';
say join(",\n",@edges);
say ']}}';


sub check_vertex{
    my %vertex2name = reverse %name2vertex;
    #          edge_id / vertexName
    while (my ($k,$v)=each %wantedVertices){
	    unless($createdVertices{$k}){
	    	die("did not create $k/$vertex2name{$k} wanted $v\n")
	    }
    }
}

sub check_edges{
  my @e=@_;
  my %vertex2name = reverse %name2vertex;
  foreach my $e(@e){
	  $e=~/"_inV":(\d+)/;
	  my $inV="$1";
	  $e=~/"_outV":(\d+)/;
          my $outV="$1";

	  unless ($createdVertices{$inV} && $createdVertices{$outV} && $vertex2name{$inV} && $vertex2name{$outV}){
		  die("can't find _inV: $inV/$vertex2name{$inV}($wantedVertices{$inV}) or _outV: $outV/$vertex2name{$outV}($wantedVertices{$outV}) ($e)\n");
	  }
  }
}


# Paper section
sub print_paper{
 $db->timestamps(1);
 my @vertex;
 my $pIt = $db->fetch_many(-class =>'Paper');
 while(my $r = $pIt->next){
  my $name = "$r";

  my $vertexID=$name2vertex{"$r"}||$vertC++;
  $name2vertex{"$r"}||=$vertexID;

  my $title=$r->Title||'';
  $title=~s/'/''/g;
  $title=~s/"/''/g;
  $title=~s/\\//g;

  my $journal=$r->Journal||'';
  my $volume=$r->Volume||'';
  my $page=$r->Page||'';
  my $citation=$r->Brief_citation||'';

  my $abstract=$r->Abstract?$r->Abstract->asAce : ''; # LongText
  $abstract=~s/LongText : "WBPaper\d+"\n"//;
  $abstract=~s/\s*"\s*$//g;
  $abstract=~s/\n//g;
  $abstract=~s/"/'/g;
  $abstract=~s/\\//g;
  $journal=~s/"/'/g;
  $journal=~s/\\//g;
  $citation=~s/"/'/g;
  $citation=~s/\\//g;

  my ($created) = join(' ',&a2cTimestamp($r));
  my $author='';
  if ($r->Author){
    my @authors = $r->Author;
    $author.=join(',',@authors);
  }

  push @vertex, qq({"AceClass":"Paper","name":"$r","_id":"$vertexID","_type":"vertex","created":"$created","title":"$title","journal":"$journal","Volume":"$volume","Page":"$page","brief_citation":"$citation","abstract":"$abstract","author":"$author"});

  foreach my $rn ($r->RNAi){
    my $_id = $edgeC++;
    my $outV = $name2vertex{$rn}||$vertC++;
    $name2vertex{"$rn"}=$outV;
    my $created = join(' ',&a2cTimestamp($rn));
    push @edges, qq({"_id":$_id,"_type":"edge","_outV":"$outV","_inV":"$vertexID","_label":"PaperRNAi","created":"$created"});
  }

  foreach my $rn ($r->Gene){
    my $_id = $edgeC++;
    my $outV = $name2vertex{$rn}||$vertC++;
    $name2vertex{"$rn"}=$outV;
    my $created = join(' ',&a2cTimestamp($rn));
    push @edges, qq({"_id":"$_id","_type":"edge","_outV":"$outV","_inV":"$vertexID","_label":"PaperGene","created":"$created"});
  }

 }
 say join(",\n",@vertex);
 $db->timestamps(0);
}

# Phenotype section
sub print_phenotype{
 $db->timestamps(1); #to get timestamps
 my @vertex;
 my $pIt = $db->fetch_many(-class =>'Phenotype');
 while(my $r = $pIt->next){
  my $name = "$r";
  my $pName = $r->Primary_name||'';
  my $description=$r->Description||'';  
  $description=~s/'/''/g;
  my ($created) = join(' ',&a2cTimestamp($r));

  my $vertexID=$name2vertex{$name}||$vertC++;
  $name2vertex{$name}||=$vertexID;

  push @vertex, qq({"AceClass":"Phenotype","name":"$name","_id":"$vertexID","_type":"vertex","created":"$created","primary_name":"$pName","description":"$description"});

  foreach my $rn ($r->RNAi){
    my $_id = $edgeC++;
    my $outV = $name2vertex{"$rn"}||$vertC++;
    $name2vertex{"$rn"}=$outV;
    my $created = join(' ',&a2cTimestamp($rn));
    push @edges, qq({"_id":"$_id","_type":"edge","_outV":"$outV","_inV":"$vertexID","_label":"PhenotypeRNAi","created":"$created"});
  }

  foreach my $rn ($r->Not_in_RNAi){ # segfaults in the copy step :-(
     my $_id = $edgeC++;
     my $outV = $name2vertex{"$rn"}||$vertC++;
     $name2vertex{"$rn"}=$outV;
     my $created = join(' ',&a2cTimestamp($rn));
     push @edges, qq({"_id":"$_id","_type":"edge","_outV":"$outV","_inV":"$vertexID","_label":"PhenotypeNot_in_RNAi","created":"$created"});
  }
 }
 say join(",\n",@vertex);
 $db->timestamps(0);
}


# RNAi section
sub print_rnai{

 my @vertex;
 $db->timestamps(1); #to get timestamps
 my $rnaIt = $db->fetch_many(-class =>'RNAi');
 while(my $r = $rnaIt->next){
  my ($created) = join(' ',&a2cTimestamp($r));
  my $strain = $r->Strain;
  my $delivered_by = $r->Delivered_by;
  my $vertexID=$name2vertex{"$r"}||$vertC++;
  $name2vertex{"$r"}=$vertexID;

  # Evidence 
  my $evidence='';
  if ($r->Evidence){
   my @e=$r->at('Evidence');
   my @b = map {"'${\$_->asString}'"} @e;
   map {s/\n//g}@b;
   $evidence=join(',',@b);
  }
  $evidence=~s/\n//g;

  push @vertex, qq({"AceClass":"RNAi","name":"$r","_id":"$vertexID","_type":"vertex","created":"$created","strain":"$strain","delivered_by":"$delivered_by","evidence":"$evidence"});

  # that will be an edge to Paper
  if ($r->Reference){
    my @Rs=$r->Reference;
    foreach my $rn(@Rs){
        my $_id = $edgeC++;
	my $outV = $name2vertex{"$rn"}||$vertC++;
	$name2vertex{"$rn"}=$outV;
        my $created = join(' ',&a2cTimestamp($rn));
        # that needs somehow a created stamp            
	push @edges, qq({"_id":"$_id","_type":"edge","_outV":"$outV","_inV":"$vertexID","_label":"RNAiReference","created":"$created"});
    }
  }

  # edge to gene
  if ($r->Gene){
    my @e=$r->at('Inhibits.Gene');
    foreach my $rn(@e){
	my $_id = $edgeC++;
	my $outV = $name2vertex{"$rn"}||$vertC++;
        my $created = join(' ',&a2cTimestamp($rn));
	$name2vertex{"$rn"}=$outV;
        my $evidence = $rn->right(2)->asString;
	$evidence=~s/\n//g;
	push @edges, qq({"_id":"$_id","_type":"edge","_outV":"$outV","_inV":"$vertexID","_label":"RNAiGene","created":"$created","evidence":"$evidence"});
    }
  }
  # edge to phenotype
  if ($r->Phenotype){
    foreach my $rn ($r->Phenotype){
	my $_id = $edgeC++;
	my $outV = $name2vertex{"$rn"}||$vertC++;
	$name2vertex{"$rn"}=$outV;
        my $created = join(' ',&a2cTimestamp($rn));
	push @edges, qq({"_id":"$_id","_type":"edge","_outV":"$outV","_inV":"$vertexID","_label":"RNAiPhenotype","created":"$created"});
    }
  }

  # another edge to phenotype
  if ($r->Phenotype_not_observed){
    foreach my $rn($r->Phenotype_not_observed){
	my $_id = $edgeC++;
	my $outV = $name2vertex{"$rn"}||$vertC++;
	$name2vertex{"$rn"}=$outV;
        my $created = join(' ',&a2cTimestamp($rn));
	push @edges, qq({"_id":"$_id","_type":"edge","_outV":"$outV","_inV":"$vertexID","_label":"RNAiPhenotype_not_observed","created":"$created"});
    }
  }
 }
 say join(",\n",@vertex);
 $db->timestamps(0);
}



sub print_genes{
  $db->timestamps(1);
  my @vertex;
  my $genIt = $db->fetch_many(-class =>'Gene');
  while(my $g = $genIt->next){
   my $name = "$g";
   my $vertexID=$name2vertex{$name}||$vertC++;
   $name2vertex{$name}||=$vertexID;

   my ($created) = join(' ',&a2cTimestamp($g));
   my $cgc_name = $g->CGC_name||'';
   my $public_name = $g->Public_name;
   my $seq_name = $g->Sequence_name;

   my $conciseDescription=$g->Concise_description||'';
   $conciseDescription=~s/'/''/g;
   
   # hrmpf
   my $cDEvidence="{}";
   if ($conciseDescription){
    $cDEvidence='{';
    my @e = $g->at('Structured_description.Concise_description[2]');
    my @lines = map {"'".join(" ",$_->row)."'"} @e;
    $cDEvidence.=join(',',@lines);
    $cDEvidence.='}';
   }

  push @vertex,qq({"AceClass":"Gene","name":"$name","_id":"$vertexID","_type":"vertex","created":"$created","CGC_name":"$cgc_name","Public_name":"$public_name","Concise_description":"$conciseDescription"});

  ############
  # these need to become edges -> RNAi_result

  if ($g->RNAi_result){
    my @e=$g->at('Experimental_info.RNAi_result');
    foreach my $rn (@e){
        my $e = $rn->right->asString;
        $e =~s/\n//g;
        my $_id = $edgeC++;
	my $outV = $name2vertex{"$rn"}||$vertC++;
	$name2vertex{"$rn"}=$outV;
        my $created = join(' ',&a2cTimestamp($rn));
        # that needs somehow a created stamp
	push @edges, qq({"_id":"$_id","_type":"edge","_outV":"$outV","_inV":"$vertexID","_label":"GeneRNAi_result","created":"$created","evidence":"$e","to":"$rn","from":"$name"});
    }
  }

  # edge -> Reference
  if ($g->Reference){
    foreach my $ref ($g->Reference){
        my $_id = $edgeC++;
	my $outV = $name2vertex{$ref}||$vertC++;
	$name2vertex{"$ref"}=$outV;
        my $created = join(' ',&a2cTimestamp($ref));
	push @edges, qq({"_id":"$_id","_type":"edge","_outV":"$outV","_inV":"$vertexID","_label":"GeneReference","created":"$created"});
    }
  }
 }
 say join(",\n",@vertex);
 $db->timestamps(0);
}

sub a2cTimestamp{
   my ($ace)=@_;
   my $ts=$ace->timestamp;
   $ts=~s/_/ /g;
   $ts=~/(.*)\s(\w+)$/;
   my $t="$1";
   my $u="$2";
   $t||=&getLoggingTime();
   $u||=$ENV{USER};
   return $t,$u;
}
sub getLoggingTime {

    my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst)=localtime(time);
    my $nice_timestamp = sprintf ( "%04d-%02d-%02d %02d:%02d:%02d",
                                   $year+1900,$mon+1,$mday,$hour,$min,$sec);
    return $nice_timestamp;
}
