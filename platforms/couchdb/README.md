Modelling
---------

CouchDB is unashamedly a document store.  For data which divides
neatly into documents, and access patterns which are typically at the
granularity of "retrieve one document by it's ID", it is extremely
straightforward (and should be fast).  It's possible to index your
documents fairly arbitrarily using the view system.  However, joins
are not CouchDB's strong point, and in particular it has no real
support for modelling and efficiently traversing many-to-many
relationships.

For version 1, I've gone for a nearly 1:1 mapping of ACeDB objects to
CouchDB documents.  This may not be optimal (see below...).  One exception
is the LongText class -- these have all been rolled into their referring
objects (in this case, always Papers).  This version doesn't have any
per-node timestamps or other metadata, but there is per-document versioning
which you get "for free" from CouchDB.  Given a commitment to CouchDB, there
may be an argument for thinking carefully about document granularity in order
to maximize the amount of metadata that can be managed on a per-document
basis rather than having to decorate every document tree with extra
metadata nodes.


Prerequisites
-------------

You'll need at least Java 6.  On platforms where you have a choice,
*always* install Oracle Java in preference to any other version.

You'll need [Leiningen](http://leiningen.org/).  The downloadable for this is just
a shell script, put it somewhere convenient on your PATH.

You'll need Apache CouchDB.  I used version 1.6.  For quick testing, it should be fine
to just run a default configuration on localhost.

Loading data
-------------

(Assumes that you've unpacked the ".ace" files in the data directory).

     (use 'wb.import-couch)
     (use 'com.ashafa.clutch)
     (get-database "smallace")    ; Creates if doesn't already exist

     (bulk-update "smallace"
       (import-acefiles
         (for [blk (range 1 11)] 
	   (str "../../data/dump_2014-06-30_A." blk ".ace"))))

Then load the design document:

      curl -X PUT http://localhost:5984/smallace/_design/smallace --data-binary @design.json 

Metrics
-------

1. How long did it take developer to get up to speed on back-end storage?

   The basics are straightforward -- fire up a database and store some documents in
   minutes, at the most.  Getting the basics of the map-reduce view/index system takes
   an hour or two if you know a bit of Javascript (also possible to write views in other
   languages, but that adds a little complexity).  Making optimal use of views looks
   like a much bigger undertaking (and conversations with others who have used CouchDB
   in anger support this).

2. How long did it take to write the importer script?

   Circa 4 hours, but reused ideas and some code from the Datomic importer.  Doing it
   from scratch would certainly take longer, I'd guess closer to 8 hours.

3. How long does the load take?

   - 123 seconds to load .ace files and create CouchDB-compatible documents
   - 38 seconds to load into CouchDB
   - ??? seconds to materialize views (roughly equivalent to creating indices)

4. Updating 10,000 concise descriptions? (choose a gene at random, set description, and write it back)

   ```
   (use 'wb.update-couch)
   (time (update-gene-descriptions "smallace" 10000))
   "Elapsed time: 82906.651 msecs"
   ```

   Need to try bulk document API as well.


5. Add 10,000 Phenotypes and RNAI (random phenotype, random gene, new RNAi, add phenotype to gene via RNAi)

   ```
   (time (make-random-rnais "smallace" "test3_" 10000))
   "Elapsed time: 203289.603 msecs"
   ```

6. Web page loading time.   10,000 random gene pages.

   Imperfect but comparable to the current Datomic test:

       ```
       (def gids (mapv :_id (get-genes "smallace")))

       (time (dotimes [n 10000] 
          (http/get 
            (str "http://localhost:8103/gene-phenotypes/" 
                 (rand-nth gids)))))
       "Elapsed time: 209203.374 msecs"
       ```

   Ouch.  Need to look into optimisations...

7. How long did it take to write the website API?

   TBD
     

Qualitative Considerations
--------------------------

1. Is the documentation good?

   The text of a book is available on the web-site.  It looks quite
   professional, but I found the chapters on views and querying a
   little bit thin.  It's possible that things get fleshed out in
   later chapters (The "...for SQL jockeys" chapter was quite
   helpful).

2. Are there user groups?  Community?

   Active (~200msgs/month on user list) mailing list, hosted by Apache project.
   Active on StackOverflow
   Subjectively: one of the highest-profile NoSQL platforms (based on mentions on
   Hacker News, etc.)

3. Is the sofware updated?

   Yes.  1.5 in November 2013, 1.6 in June 2014.  Twice-yearly major releases seem
   to be typical.

4. Is there a help forum?  Are there answers?

   Mailing list, stack overflow.

5. Lanugage bindings:

   All clients use the same REST API.  It's also possible to write view servers
   in arbitrary languages (although Javascript is what's supported out of the box
   and seems like the standard choice).

   Perl: http://search.cpan.org/dist/CouchDB-Client/lib/CouchDB/Client.pm

   Javascript: REST API directly, or a choice of several helper libraries

   Python: http://github.com/djc/couchdb-python

   Java: several, e.g. http://www.lightcouch.org/

   SQL: N/A

   C/C++: several

6. Licensing?  Commercial support?  Pricing?

   Apache License 2.0 (i.e. liberal open source)

   Some supported commercial forks, e.g. https://cloudant.com/product/pricing/ (recently
   acequired by IBM), http://www.couchbase.com/

7. Scalability?

   TBD.
