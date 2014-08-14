Modelling
---------

Neo4J is a graph database: data is stored as "nodes" and "edges".
Edges are always associate with two nodes (no hypergraph shenanigans
as far as I can tell).  Edges are directed, but it's possible to
write queries which ignore edge direction, which might come in handy
in some cases (edges between paralogues?)

Both nodes and edges can have arbitary key-value properties.
Properties can be scalars (strings, numbers, etc.) or lists of
scalars.  However, this is *not* a document-store in the modern sense:
properties cannot have maps as values, so the ability of a single node
or edge to store structured data is limited.

In smallace the amount of "internal" object structure isn't huge,
so we've been able to keep a mostly-1:1 mapping between ACeDB objects
and Neo4J nodes -- although not certain this will remain true with
the full Wormbase.  Where there is internal structure, it is generally
to associate metadata with a relationship (evidence for a Gene->RNAi
relationship, "observed" or "not observed" for an RNAi->Phenotype
relationship).  These logically fit into properties on the edges
of the graph.


Prerequisites
-------------

You'll need at least Java 7.  On platforms where you have a choice,
*always* install Oracle Java in preference to any other version.

You'll need [Leiningen](http://leiningen.org/).  The downloadable for
this is just a shell script, put it somewhere convenient on your PATH.

You'll need Neo4J.  I used version 2.1.3.  For quick testing, it
should be fine to just do a "neo4j start" with default configuration
on localhost.

Finally, you'll need a Clojure command prompt ("REPL") at which to
issue some of these commands.  Easiest way to do this is to type "lein
repl" in the probject directory.  The first time you do this, there
will be a slight delay as some dependencies (defined in the
project.clj file) are downloaded.

Loading data
-------------

(Assumes that you've unpacked the ".ace" files in the data directory).

     (use 'wb.import-neo)
     (require '[clojurewerkz.neocons.rest :as nr])

     (def conn (nr/connect "http://localhost:7474/db/data"))

     (time
       (import-acefiles conn
         (for [b (range 1 11)]
	   (str "../../data/dump_2014-06-30_A." b ".ace"))))

There is some feedback as you go.  Note that this import is currently
quite slow, even on a high-spec machine.

Metrics
-------

1. How long did it take developer to get up to speed on back-end storage?

   ~8 hours

2. How long did it take to write the importer script?

   10 hours (with some sharing of code/ideas with Datomic and CouchDB -- probably add 3-4 hours
   if I hadn't had a head start).

   Possibly the tooling I used ([Neocons](http://clojureneo4j.info/))
   was a bad choice.  Although it looks quite polished at first glace,
   it's missing a good API for bulk imports, and I had a nightmarish
   debugging session working out what was going on when the server
   couldn't quite keep up with the rate of requests the importer was
   issuing.

3. How long does the load take?


4. Updating 10,000 concise descriptions? (choose a gene at random, set description, and write it back)


5. Add 10,000 Phenotypes and RNAI (random phenotype, random gene, new RNAi, add phenotype to gene via RNAi)


6. Web page loading time.   10,000 random gene pages.


7. How long did it take to write the website API?


     

Qualitative Considerations
--------------------------

1. Is the documentation good?


2. Are there user groups?  Community?

   [Active Google Group](https://groups.google.com/forum/#!forum/neo4j)

3. Is the sofware updated?

   [Frequently](http://neo4j.com/release-notes/) -- in particular, 2.0
   was a very major change.

4. Is there a help forum?  Are there answers?

   [Active Google Group](https://groups.google.com/forum/#!forum/neo4j)

   Most posts seem to get answered.

5. Lanugage bindings:

   Probably all languages will need to go through the REST API.  There is an option
   to run it in-process for JVM languages, but the licensing situation seems a bit
   hairy.

   Perl: http://search.cpan.org/~majensen/REST-Neo4p-0.126/lib/REST/Neo4p.pm

   Javascript: http://www.neo4j.org/develop/javascript

   Python: http://www.neo4j.org/develop/python

   Java: http://www.neo4j.org/develop/java

   SQL: N/A

   C/C++: https://github.com/forhappy/neo4j-cpp-driver

6. Licensing?  Commercial support?  Pricing?

   http://neo4j.com/subscriptions/

   Community edition is GPLed.  Support contracts available (but probably
   not cheap!)

7. Scalability?

   TBD.
