Modelling process
-----------------

OrientDB is a multi-paradigm database, supported "graph" and "document"
(and potentially also hybrid) models.  This is explicitly a graph-based
approach, using the Tinkerpop Blueprints interface supported by a number
of different GraphDB.  Following discussions with @a8wright, I've been
experimentign with graphs while he is working on something more document-
oriented.

Loading data
------------

(require '[archimedes.core :as g])
(require '[archimedes.vertex :as v])
(require '[archimedes.edge :as e])

(import 'com.tinkerpop.blueprints.impls.orient.OrientGraph)
(g/set-graph! (OrientGraph. "remote:localhost/smallace"))

(use 'wb.import-orient-tinker :reload)
(time (import-acefiles (for [b (range 1 11)] (str "../../data/dump_2014-06-30_A." b ".ace")))))

; Example query
(require '[ogre.core :as q])
(q/query (v/find-by-kv :_id "WBGene00000100")
         (q/--> [:rnai])
         (q/--> [:phenotype-observed])
         q/into-vec!)

Metrics
-------

1. How long did it take developer to get up to speed on back-end storage?


2. How long did it take to write the importer script?

   Circa 6 hours (with some code reuse from previous implementations)

3. How long does the load take?

   TBD

4. Updating 10,000 concise descriptions? (choose a gene at random, set
description, and write it back)

   TBD


5. Add 10,000 Phenotypes and RNAI (random phenotype, random gene, new RNAi, add phenotype to gene via RNAi)

   TBD

6. Web page loading time.   10,000 random gene pages.

   TBD

7. How long did it take to write the website API?

   TBD
     

Qualitative Considerations
--------------------------

1. Is the documentation good?

   TBD

2. Are there user groups?  Community?

   TBD

3. Is the sofware updated?

   Frequently

4. Is there a help forum?  Are there answers?

   TBD

5. Lanugage bindings:

   Perl: @a8wright working on OrientDB binary driver.  Also some
         Tinkerpop support?

   Javascript: Several different HTTP-based options.  Also binary driver
               for NodeJS

   Python: 

   Java: Native library or Tinkerpops Blueprints

   SQL: OrientDB supports an SQL-based query language (with
        significant extensions, especially when working on graphs).
        Java programs can access this via a JDBC driver -- presumably
        a Perl DBD driver would also be possible.

   C/C++: Binary driver available

6. Licensing?  Commercial support?  Pricing?

   Apache2 License

7. Scalability?

   TBD