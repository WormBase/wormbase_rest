-- :name gene-features :? :1
-- :doc retrieve all sequences for a gene by id
SELECT f.id,f.object,f.typeid,f.seqid,f.start,f.end,f.strand FROM feature as f JOIN name as n ON n.id=f.id
WHERE n.name = :name
