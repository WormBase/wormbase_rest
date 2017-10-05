-- :name get-features :? :*
-- :doc Retrieve all sequences for a feature by id
SELECT f.id,f.typeid,t.tag as type,f.seqid,l.seqname,f.start,f.end,f.strand
FROM feature as f
JOIN name as n ON n.id=f.id
JOIN locationlist as l ON l.id=f.seqid
JOIN typelist as t ON t.id=f.typeid
WHERE n.name = :name

-- :name sequence-features-where-type :? :*
-- :doc Retrieve all sequences for a feature by id whre of type transcript, CDS, mRNA or scpecified type
SELECT f.id,f.typeid,t.tag as type,f.seqid,l.seqname,f.start,f.end,f.strand
FROM feature as f
JOIN name as n ON n.id=f.id
JOIN locationlist as l ON l.id=f.seqid
JOIN typelist as t ON t.id=f.typeid
WHERE n.name = :name
AND (t.tag LIKE "transcript%"
    OR t.tag LIKE "CDS%"
    OR t.tag LIKE "mRNA%"
    or t.tag = :tag)
