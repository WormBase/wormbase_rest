-- :name gene-features :? :*
-- :doc retrieve all sequences for a gene by id
SELECT f.id,f.object,f.typeid,f.seqid,l.seqname,f.start,f.end,f.strand
FROM feature as f
JOIN name as n ON n.id=f.id
JOIN locationlist as l ON l.id=f.seqid
WHERE n.name = :name
