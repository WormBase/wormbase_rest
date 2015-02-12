#!/usr/bin/env perl

use strict;
use edn;
use HTTP::Tiny;
use Data::Dumper;

my ($gene_id) = @ARGV;
die "Please specify a gene ID, e.g. WBGene00004013.\n" unless $gene_id;

my $query = <<'END_QUERY';
   [:find (pull ?gt [{:gene.go-term/go-term [
                          :go-term/id 
                          :go-term/term 
                          {:go-term/type [:db/ident]}] 
                      :gene.go-term/go-code [
                          :go-code/id 
                          :go-code/description]} 
                     :evidence/date-last-updated 
                     :evidence/inferred-automatically 
                     {:evidence/curator-confirmed [
                          :person/id 
                          :person/standard-name] 
                      :evidence/paper-evidence [
                          :paper/id 
                          {:paper/person [
                              {:paper.person/person [
                                  :person/id 
                                  :person/standard-name]}]}]}])
     :in $ ?gid 
     :where [?g :gene/id ?gid] [?g :gene/go-term ?gt]]
END_QUERY
    
my $dbargs = edn::write(
    [
     {'db/alias' => 'ace/wb244-imp2'},
     $gene_id
    ]
);

my $resp = HTTP::Tiny->new->post_form(
    'http://localhost:4664/api/query',
    {q => $query,
     args  => $dbargs});
my $overview = edn::read($resp->{'content'});

print Dumper($overview); 
