require 'rubygems'
require 'sinatra'
require 'sinatra/json'
require 'diametric'

set :server, 'webrick'

$conn = Diametric::Persistence::Peer.connect('datomic:free://localhost:4334/wb248-imp1')
$db = $conn.db.to_java

# convert gene datomic data into an anonymous hash
def pack_gene(g)
  {:id       => g.get(':gene/id'),
   :label    => g.get(':gene/public-name'),
   :class    => 'gene',
   :taxonomy => 'c_elegans'}
end

# convert person datomic data into an anonymous hash
def pack_person(p)
  {:id       => p.get(':person/id'),
   :label    => p.get(':person/standard-name'),
   :class    => 'person',
   :taxonomy => 'all'}
end

# sinatra url match / get
get '/rest/widget/gene/:gid/history' do
  gene = $db.entity([':gene/id', params['gid']]) #grab the gene based on the geneid
  json(
    {:name   => params['gid'],
     :class  => 'gene',
     :uri    => 'whatevs',
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
                  :action  => 'Unknown'}

             merged   = h.get(':gene-history-action/merged-into')
             acquires = h.get(':gene-history-action/acquires-merge')
             imported = h.get(':gene-history-action/imported')

             if h.get(':gene-history-action/created')
               r[:action] = 'Created'
             elsif h.get(':gene-history-action/killed')
               r[:action] = 'Killed'
             elsif h.get(':gene-history-action/suppressed')
               r[:action] = 'Suppressed'
             elsif h.get(':gene-history-action/resurrected')
               r[:action] = 'Created'
             elsif h.get(':gene-history-action/transposon-in-origin')
               r[:action] = 'Transposon_in_origin'
             elsif h.get(':gene-history-action/changed-class')
               r[:action] = 'Changed_class'
               # What to do with old/new class?
             elsif merged
               r[:action] = 'Merged_into'
               r[:gene] = pack_gene(merged)
             elsif acquires
               r[:action] = 'Acquires_merge'
               r[:gene] = pack_gene(acquires)
             elsif imported
               r[:action] = 'Imported'
               r[:remark] = imported.iterator.next;
             end
               
             r # return itself, as the object is not in the last line of the block
          },
          :description => 'the historical annotations of this gene'
        }
    }})
end
