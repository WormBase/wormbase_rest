#!/usr/bin/env perl

use strict;
use edn;
use HTTP::Tiny;
use Data::Dumper;

my ($gene_id) = @ARGV;
die "Please specify a gene ID, e.g. WBGene00004013.\n" unless $gene_id;

my $query = <<'END_QUERY';
    [:find (pull ?g [:gene/id
                     :gene/transposon-in-origin
                     :gene/other-name
                     :gene/molecular-name
                     :gene/sequence-name
                     :gene/public-name
                     :gene/legacy-information
                     :gene/remark
                     {:gene/cgc-name [
                         :gene.cgc-name/text 
                         {:evidence/person-evidence [
                              :person/id 
                              :person/standard-name]}]
                      :gene/species [:species/id]
                      :gene/gene-class [
                         :gene-class/id 
                         :gene-class/description]
                      :clone/_positive-gene [:clone/id]
                      :sequence.gene-child/_gene [
                         {:sequence/_gene-child [:sequence/id]}]
                      :gene/provisional-description [
                          :gene.provisional-description/text
                          :evidence/date-last-updated
                          {:evidence/curator-confirmed [:person/id]
                           :evidence/paper-evidence [
                             :paper/id
                             {:paper/person [
                                {:paper.person/person [
                                   :person/id 
                                   :person/standard-name]}]}]}]
                      :operon.contains-gene/_gene [{:operon/_contains-gene [:operon/id]}]
                      :gene/corresponding-transposon [{:gene.corresponding-transposon/transposon [:transposon/id]}]}])
    :in $ ?gid 
    :where [?g :gene/id ?gid]]
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
