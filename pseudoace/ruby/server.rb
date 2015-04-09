require 'rubygems'
require 'sinatra'
require 'sinatra/json'
require 'diametric'

set :server, 'webrick'

$conn = Diametric::Persistence::Peer.connect('datomic:free://localhost:4334/wb248-imp1')
$db = $conn.db.to_java

def pack_gene(g)
  {:id       => g.get(':gene/id'),
   :label    => g.get(':gene/public-name'),
   :class    => 'gene',
   :taxonomy => 'c_elegans'}
end

def pack_person(p)
  {:id       => p.get(':person/id'),
   :label    => p.get(':person/standard-name'),
   :class    => 'person',
   :taxonomy => 'all'}
end

get '/rest/widget/gene/:gid/history' do
  gene = $db.entity([':gene/id', params['gid']])
  json(
    {:name   => params['gid'],
     :class  => "gene",
     :uri    => "whatevs",
     :fields => {
       :name => {
          :data => pack_gene(gene),
          :description => "The name and WormBase internal ID of #{gene.get(':gene/id')}"
       },
       :history => {
         :data  => gene.get(':gene/version-change').map { |h| 
             r = {:version => h.get(':gene.version-change/version'),
                  :date    => h.get(':gene.version-change/date').toString,
                  :curator => pack_person(h.get(':gene.version-change/person')),
                  :remark  => nil,
                  :gene    => nil,
                  :action  => "Unknown"}

             if h.get(':gene-history-action/created')
               r['action'] = 'Created'
             end

             if h.get(':gene-history-action/killed')
               r['action'] = 'Killed'
             end

             if h.get(':gene-history-action/suppressed')
               r['action'] = 'Suppressed'
             end

             if h.get(':gene-history-action/resurrected')
               r['action'] = 'Created'
             end

             merged = h.get(':gene-history-action/merged-into')
             if merged
               r['action'] = 'Merged_into'
               r['gene'] = pack_gene(merged)
             end

             acquires = h.get(':gene-history-action/acquires-merge')
             if acquires
               r['action'] = 'Acquires_merge'
               r['gene'] = pack_gene(acquires)
             end

             imported = h.get(':gene-history-action/imported')
             if imported
                 r['action'] = 'Imported'
                 r['remark'] = imported.iterator.next;
             end

             if h.get(':gene-history-action/changed-class')
               r['action'] = 'Changed_class'
               # What to do with old/new class?
             end

             if h.get(':gene-history-action/transposon-in-origin')
               r['action'] = 'Transposon_in_origin'
             end
               
             r
         },
         :description => "the historical annotations of this gene"
       }
    }})
end
