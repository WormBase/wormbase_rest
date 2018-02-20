-- :name get-features :? :*
-- :doc Retrieve all sequences for a feature by id
SELECT f.id,f.typeid,t.tag as type,f.seqid,l.seqname,f.start,f.end,f.strand
FROM feature as f
JOIN name as n ON n.id=f.id
JOIN locationlist as l ON l.id=f.seqid
JOIN typelist as t ON t.id=f.typeid
WHERE n.name = :name

-- :name get-attribute-id-by-name :? :*
-- :doc Retrieve all sequences for a feature by id
SELECT id
FROM attribute
WHERE attribute_value= :name

-- :name get-features-by-id :? :*
-- :doc Retrieve all sequences for a feature by id
SELECT f.id,f.typeid,t.tag as type,f.seqid,l.seqname,f.start,f.end,f.strand
FROM feature as f
JOIN locationlist as l ON l.id=f.seqid
JOIN typelist as t ON t.id=f.typeid
WHERE f.id = :id

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

-- :name variation-features :? :*
-- :doc Retrieve all sequences for a gene by ida
SELECT f.id,CONVERT(f.object USING utf8),f.typeid,f.seqid,f.start,f.end,f.strand
FROM feature as f
JOIN attribute as a ON a.id=f.id
JOIN attributelist as al ON al.id=a.attribute_id
WHERE a.attribute_value = "WBVar00101112"
AND al.tag = "variation"
AND f.object NOT LIKE "%PCoF%";
