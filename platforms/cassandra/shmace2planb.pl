#!/usr/bin/env perl
# convert ShmACE to CSQL
# shmace2planb <shmace>

use Ace;
use feature qw(say);

my $db = Ace->connect(-path => shift())||die(Ace->error);

&print_genes;
&print_rnai;
&print_phenotype;
&print_paper;

sub print_genes{
 # Gene section
 my $genIt = $db->fetch_many(-class =>'Gene');
 while(my $g = $genIt->next){
  my $name = $g;
  my $created = &getLoggingTime;
  my $cgc_name = $g->CGC_name||'';
  my $public_name = $g->Public_name;

  # that is for set<text>
  my $conciseDescription=$g->Concise_description||'';
  $conciseDescription=~s/'/''/g;
  my $cDEvidence="{}";
  if ($conciseDescription){
    $cDEvidence='{';
    my @e = $g->at('Structured_description.Concise_description[2]');
    my @lines = map {"'".join(" ",$_->row)."'"} @e;
    $cDEvidence.=join(',',@lines);
    $cDEvidence.='}';
  }

  # that is for map<text>
  my $rnai='{}';
  if ($g->RNAi_result){
    $rnai='{';
    my @e=$g->at('Experimental_info.RNAi_result');
    my @b = map {"'$_': '${\$_->right->asString}'"} @e;
    map {s/\n//g}@b;
    $rnai.=join(',',@b);
    $rnai.='}';
  }

  my $reference='{}';
  if ($g->Reference){
    $reference='{';
    $reference.=join(',',map{"'$_'"}$g->Reference);
    $reference.='}';
  }

  say 'INSERT INTO genes(Name,created,CGC_name,Public_name,Concise_description,Concise_descriptionEvidence,RNAi_result,Reference)';
  say "VALUES('$g','$created','$cgc_name','$public_name','$conciseDescription',$cDEvidence,$rnai,$reference);";
 }
}

# RNAi section
sub print_rnai{
 my $rnaIt = $db->fetch_many(-class =>'RNAi');
 while(my $r = $rnaIt->next){
  my $name = $r;
  my $created = &getLoggingTime;
  my $strain = $r->Strain;
  my $delivered_by = $r->Delivered_by;


  # that is for set<text>
  my $reference='{}';
  if ($r->Reference){
    my @Rs=$r->Reference;
    @Rs = map {"'$_'"} @Rs;
    $reference='{'.join(',',@Rs).'}';
  }

  # Evidence
  my $evidence='{}';
  if ($r->Evidence){
   $evidence='{';
   my @e=$r->at('Evidence');
   my @b = map {"'${\$_->asString}'"} @e;
   map {s/\n//g}@b;
   $evidence.=join(',',@b);
   $evidence.='}';
  }

  # that is for map<text>
  my $gene='{}';
  if ($r->Gene){
    $gene='{';
    my @e=$r->at('Inhibits.Gene');
    my @b = map {"'$_': '${\$_->right->asString}'"} @e;
    map {s/\n//g}@b;
    $gene.=join(',',@b);
    $gene.='}';
  }
  my $phen='{}';
  if ($r->Phenotype){
    $phen='{';
    $phen.=join(',',map{"'$_'"}$r->Phenotype);
    $phen.='}';
  }
  my $phenNOT='{}';
  if ($r->Phenotype_not_observed){
    $phenNOT='{';
    $phenNOT.=join(',',map{"'$_'"}$r->Phenotype_not_observed);
    $phenNOT.='}';
  }

  say 'INSERT INTO RNAi(Name,created,strain,Delivered_by,Evidence,Gene,Phenotype,Phenotype_not_observed,Reference)';
  say "VALUES('$r','$created','$strain','$delivered_by',$evidence,$gene,$phen,$phenNOT,$reference);";
 }
}

# Phenotype section
sub print_phenotype{
 my $pIt = $db->fetch_many(-class =>'Phenotype');
 while(my $r = $pIt->next){
  my $name = "$r";
  my $pName = $r->Primary_name||'';
  my $description=$r->Description||'';  
  $description=~s/'/''/g;

  # that is for set<text>

  my $rnai='{}';
  if ($r->RNAi){
    $rnai='{';
    $rnai.=join(',',map{"'$_'"}$r->RNAi);
    $rnai.='}';
  }

  my $rnaiNOT='{}';
  if ($r->Not_in_RNAi){
    $rnaiNOT='{';
    $rnaiNOT.=join(',',map{"'$_'"}$r->Not_in_RNAi);
    $rnaiNOT.='}';
  }

  say 'INSERT INTO Phenotype(Name,Primary_name,Description,RNAi,Not_in_RNAi)';
  say "VALUES('$r','$pName','$description',$rnai,$rnaiNOT);\n";
 }
}

# Paper section
sub print_paper{
 my $pIt = $db->fetch_many(-class =>'Paper');
 while(my $r = $pIt->next){
  my $name = "$r";
  my $author = $r->Author||'';
  $author=~s/'/''/g;

  my $title=$r->Title||'';
  $title=~s/'/''/g;

  my $journal=$r->Journal||'';
  my $volume=$r->Volume||'';
  my $page=$r->Page||'';
  my $citation=$r->Brief_citation||'';

  my $abstract=$r->Abstract?$r->Abstract->asAce : ''; # LongText
  $abstract=~s/LongText : "WBPaper\d+"\n"//;
  $abstract=~s/\s*"\s*$//g;
  $abstract=~s/\n//g;
  $abstract=~s/'/''/g;

  $journal=~s/'/''/g;
  $citation=~s/'/''/g;

  # that is for set<text>

  my $rnai='{}';
  if ($r->RNAi){
    $rnai='{';
    $rnai.=join(',',map{"'$_'"}$r->RNAi);
    $rnai.='}';
  }

  my $gene='{}';
  if ($r->Gene){
    $gene='{';
    $gene.=join(',',map{"'$_'"}$r->Gene);
    $gene.='}';
  }

  say 'INSERT INTO Paper(Name,Author,Title,Journal,Volume,Page,Brief_citation,Abstract,Gene,RNAi)';
  say "VALUES('$r','$author','$title','$journal','$volume','$page','$citation','$abstract',$gene,$rnai);\n";
 }
}

sub getLoggingTime {

    my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst)=localtime(time);
    my $nice_timestamp = sprintf ( "%04d-%02d-%02d %02d:%02d:%02d",
                                   $year+1900,$mon+1,$mday,$hour,$min,$sec);
    return $nice_timestamp;
}
