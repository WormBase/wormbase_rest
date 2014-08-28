Modelling process
-----------------

OrientDB is a multi-paradigm database, supported "graph" and "document"
(and potentially also hybrid) models.  This is explicitly a graph-based
approach, using the Tinkerpop Blueprints interface supported by a number
of different GraphDB.  Following discussions with @a8wright, I've been
experimenting with graphs while he is working on something more document-
oriented.

Arbitrary scalar properties can be attached to nodes.  There's the
possibility of defining "classes" with schemas of allowed properties,
but I haven't explored that yet, and it's not entirely clear how well
this fits with doing everything via Tinkerpop.

Unlike Neo4J, there doesn't seem to be any support for more than one
value of a simple property (i.e. attaching a list of strings to
represent the authors of a paper).  Solution is to have extra
"paper-author" nodes, since (obviously) you're allowed arbitary
numbers of out-bound edges in the graph.  Wasn't a problem to do this
for the Paper class in smallace, but it did get me worrying slightly
that an awful lot of tags in the Wormbase models are non-UNIQUE (at
least in principle, even if they generally only have a single value).
Something to think about, both in terms of DB selection and also in
terms of whether there's scope for cleaning up the Wormbase model
making more things explicitly UNIQUE/cardinality-one.

Prerequisites
-------------

You'll need at least Java 6.  Java 7 will probably perform better.  On
platforms where you have a choice, always install Oracle Java in
preference to any other version.

You'll need [Leiningen](http://leiningen.org/).  The downloadable for
this is just a shell script, put it somewhere convenient on your PATH.

You'll need [OrientDB](http://www.orientechnologies.com/).  I used
version 1.7.8.  Unpack and run the server.sh script.  You'll then need
to run console.sh and type:

     create database remote:localhost/smallace root <password> plocal

Finally, you'll need a Clojure command prompt ("REPL") at which to
issue some of these commands.  Easiest way to do this is to type "lein
repl" in the probject directory.  

Loading data
------------

```
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
         q/into-vec!)```

Metrics
-------

1. How long did it take developer to get up to speed on back-end storage?

   Circa 12 hours (but did look at both native and Tinkerpop access methods).

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

   There's plenty of it, and it's mostly comprehensive, although I ran
   into a few issues with odd corners of the native Java API.  This
   may be less of an issue if we're happy to stick with graph access
   via the Tinkerpop stack.

   One thing I did think was sorely lacking was advice on when to use
   the graph model and when to use the document model.

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

   Apache2 License.

   Commercial support available from OrienTechnologies: £3000
   for first server, £1500 for additional servers.

7. Scalability?

   TBD