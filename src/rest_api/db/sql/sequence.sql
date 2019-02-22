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
-- :doc Retrieve all sequences for a feature by id where of type transcript, CDS, mRNA or scpecified type
SELECT f.id,f.typeid,t.tag as type,f.seqid,l.seqname,f.start,f.end,f.strand
FROM feature as f
JOIN name as n ON n.id=f.id
JOIN locationlist as l ON l.id=f.seqid
JOIN typelist as t ON t.id=f.typeid
WHERE n.name = :name
AND t.tag LIKE :tag

-- :name sequence-features-where-type-gene :? :*
-- :doc Retrieve all sequences for a feature by id where of type transcript, CDS, mRNA or scpecified type
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
-- :doc Retrieve all sequences for a gene by id
SELECT f.id,f.typeid,f.seqid,l.seqname,f.start,f.end,f.strand
FROM feature as f
JOIN attribute as a ON a.id=f.id
JOIN locationlist as l on l.id=f.seqid
JOIN attributelist as al ON al.id=a.attribute_id
WHERE a.attribute_value = :name
AND al.tag = "variation"
AND f.object NOT LIKE "%PCoF%";

-- :name get-sequence :? :*
-- :doc Retrieve raw sequence from location
SELECT CONVERT(s.sequence using UTF8) as sequence
FROM sequence as s
JOIN locationlist as l ON l.id=s.id
WHERE l.seqname = :location
AND s.offset = :offset

-- :name get-seq-features :? :*
-- :doc Retreive all sequence features from transcript
SELECT tc.tag,fc.start AS start,fc.end AS stop
FROM feature as f
LEFT OUTER JOIN parent2child as pc ON pc.id=f.id
LEFT OUTER JOIN typelist as t ON t.id=f.typeid
LEFT OUTER JOIN feature as fc ON pc.child=fc.id
LEFT OUTER JOIN typelist as tc ON tc.id=fc.typeid
LEFT OUTER JOIN name as n ON n.id=f.id
WHERE n.name = :name
AND (t.tag LIKE "transcript%"
	    OR t.tag LIKE "CDS%"
	    OR t.tag LIKE "mRNA%")
