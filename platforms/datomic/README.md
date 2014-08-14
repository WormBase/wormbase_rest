Modelling process
-----------------

Every ACeDB object is a Datomic entities.  Tags in the ACeDB model map
to attributes in Datomic, with some flatting,
e.g. Gene.Identity.Name.Public_name becomes :gene/name.public.  In
cases where an internal tag can have multiple child tags (values?)
*and* can occur multiple times in the same object, Datomic child
entities are used.

One special case: since Datomic is just a bag of datoms ("facts"), it
doesn't have any concept of ordered collections.  There is one case
where this seems important in smallace: the order of authorship on a
paper.  This is also modeled using component entities, i.e.:

    {:paper/id       "WBPaper12345"
     :paper/author   [{:paper.author/ordinal   1
                       :paper.author/name "Blogs F"}
                       {:paper.author/ordinal   2
                       :paper.author/name "Public J"}]
     ; ...
    }

Clients that retrieve the :paper/author will see the authors in
arbitrary order, but can sort by the ordinals as required.  The
downside is that any and all code that stores papers needs to
understand the conventions.  Using transaction function (i.e. code
stored in the transactor) to create some or all entities would make
this more reliable, at the cost of a little extra complexity.  As a
one-off, I'd suggest this isn't a big deal, but would be worth looking
through the complete data model to see how often meaningfully-ordered
collections occur.

If I did this again, I suggest there's scope for a little more
flattening.  In particular, while keeping LongText objects as separate
entities made the conversion code more straightforward, it has no real
advantage in Datomic terms, and just attaching the text directly the
the relevant entities would simplify clients and probably improve
performance at least a little.

I didn't try reconstructing history as Datomic transactions, partly
for code simplicity, partly because there wasn't actually any
meaningful timestamp data in the smallace dumps.  However, since there
is a mostly 1:1 correspondance between tags in the AceDB dumps and
datoms in the Datomic representation, a slightly-more-sophisticated
converter could turn the ace dumps into a stream of
EAV-triple+datestamp.  Then just sort by datestamp (might need to be
done on disk unless a big-memory machine is available) and chop into
sensible transactions.

It's possible that some of the evidence tags could be moved into
transactions at the same time, but that's a matter for wider
discussion.

Prerequisites
-------------

You'll need at least Java 6.  Java 7 will probably perform better.  On
platforms where you have a choice, always install Oracle Java in
preference to any other version.

You'll need [Leiningen](http://leiningen.org/).  The downloadable for
this is just a shell script, put it somewhere convenient on your PATH.

You'll need [Datomic Free](https://my.datomic.com/downloads/free).  I
was using version 0.9.4699, but would expect newer versions to work
fine.

You'll want an instance of the Datomic Transactor running.  This is
the component which looks most like the "server" in a conventional
database architecture.  In the Free edition, the transactor includes
some in-process storage, which makes it look even more like a
conventional server.  You can run the transactor from a
freshly-unpacked Datomic download with:

       bin/transactor config/samples/free-transactor-template.properties

Finally, you'll need a Clojure command prompt ("REPL") at which to
issue some of these commands.  Easiest way to do this is to type "lein
repl" in the probject directory.  If you haven't used a LISP before,
try a few simple forms, e.g.:

       (+ 2 2)
       (range 10)
       (map inc (range 10)) ; `inc` is the increment operator

Loading data
-------------

(Assumes that you've unpacked the ".ace" files in the data directory).

    (use 'acedb.importd)
    (require '[datomic.api :as d :refer (q db)])

    (def uri "datomic:free://localhost:4334/smallace")
    (d/delete-database uri)  ; No-op if DB doesn't exist, otherwise cleans up
    (d/create-database uri)

    (def con (d/connect uri))
    (def schema (read-string (slurp "schema.edn")))
    (count @(d/transact con schema))

    (doseq [block (range 1 11)
            :let [file (str "../../data/dump_2014-06-30_A." block ".ace")]]
      (doseq [t (import-acefile file)]
        (time @(d/transact con t))))


Running the web server
----------------------

      (use 'web.server)
      (use 'ring.adapter.jetty)
      (run-jetty appn {:port 8102 :join? false})

Point your browser to:

      http://localhost:8102/gene-phenotypes/WBGene00022042

Smallace issues
---------------

 - Gene Reference tag is in the model, but doesn't appear in any of the dumps,
   therefore Gene references widget is rather uninteresting (FIXED)

 - Insufficient gene information to do anything except a very stripped down
   "overview" widget.

 - Authors tags in papers are in alphabetical order, not as listed in the
   publication.  Appears to be no way of reconstructing actual authorship
   order (FIXED)

Metrics
-------

1. How long did it take developer to get up to speed on back-end storage?

     Already knew some Datomic basics, probably 2 hours total spent learning
     more about datomic schema design and thinking how to model smallace data

2. How long did it take to write the importer script?

     Circa 8 hours, including .ace file parser and ancillaries.

3. How long does the load take?

     225 seconds, from .ace files.  About 50% of this seems to be spent applying one
     transaction, which I suspect contains too many "upserts" and is upsetting the
     transactor's temporary ID resolution code.

4. Updating 10,000 concise descriptions? (choose a gene at random, set
description, and write it back)

      ```
      (use 'acedb.update-tests)
      ; Perform each update as a separate transaction
      (time (update-gene-descriptions conn 10000))
      Elapsed time: 55238.706 msecs"

      ; Batch-update 10000 descriptions in a single transactions
      (time (count (update-gene-descriptions conn 10000)))
      "Elapsed time: 1385.387 msecs"
      ```


5. Add 10,000 Phenotypes and RNAI (random phenotype, random gene, new RNAi, add phenotype to gene via RNAi)

      ```
      (use 'acedb.update-tests)
      (time (count (make-random-rnai conn "test" 10000)))
      "Elapsed time: 9809.942 msecs"
      ```

6. Web page loading time.   10,000 random gene pages.

   Quick'n'dirty...

       ```
       (def gids (->> (q '[:find ?gid :where [?g :gene/id ?gid]] 
                         (db conn))
		      (map first)
                      (vec)))

       (time (dotimes [n 10000] 
          (http/get 
            (str "http://localhost:8102/gene-phenotypes/" 
                 (rand-nth gids)))))

       "Elapsed time: 56731.178 msecs"
       ```

   i.e 5.7ms per request.  Not a particularly realistic workload
   because each request has to be completely processed and returned
   before the next one is initiated.  Would certainly get somewhat
   higher throughput if making concurrent requests.

7. How long did it take to write the website API?

     Circa 2 hours.  NB this is first-prototype quality, and probably
     has some performance issues, e.g. using traversal APIs in places
     where a "proper" datalog query would be more optimizable.
     

Qualitative Considerations
--------------------------

1. Is the documentation good?

     Too subjective for me?  I found the tutorials very good when first
     discovered Datomic.  The reference docs were more than adequate for
     writing these scripts.  There are a few "folklore" issues (e.g. when
     to `touch` an entity), but in my experience that's common for all DB
     platforms.

2. Are there user groups?  Community?

    Active Google group: https://groups.google.com/forum/#!forum/datomic

    Datomic questions on Stack Overflow seem to get answered quickly and helpfully.

    Cambridge NonDysfunctional Programmers had an introduction-to-Datomic evening
    last year.  I missed it for family reasons, but heard that it was very good.

3. Is the sofware updated?

    Frequently.  See, e.g.: https://my.datomic.com/downloads/free

4. Is there a help forum?  Are there answers?

   Google group (see above) is active, most things seem to get answered.  Both
   Cognitect developers and users from the community answer questions.  Also,
   Stack Overflow.

5. Lanugage bindings:

   Perl: Use REST API.  Will need an EDN library.  @dasmoth will write if he
   gets free time in July.

   Javascript: REST API + https://github.com/shaunxcode/jsedn

   Python: https://github.com/gns24/pydatomic

   Java: use Datomic's native Peer library (also for any other JVM language)

   SQL: N/A

   C/C++: multiple options.  REST+EDN certainly works.  Embedding the Peer library and
   accessing via JNI would be more performant.

   Update: Cognitect have recently released the [Transit](https://github.com/cognitect/transit-format)
   format, which looks like it may largely replace EDN as the data format for the
   Datomic REST interface.  This may make life easier.  I've asked on the mailing
   list about timescales for this (and also about doing bulk entity queries over
   the REST API).

6. Licensing?  Commercial support?  Pricing?

   http://www.datomic.com/pricing.html

   It's actually possible that Datomic Pro Starter (limited to two
   peers) would suffice for Wormbase's requirements, especially if
   much of the code is accessing via the REST adapter (which counts as
   a single Peer, regardless of how many clients connent to it) -- but
   this would need exploration.

   Commercial support would probably be appreciated anyway.

7. Scalability?

   May need more exploration.  In principle, Datomic should support
   more-or-less arbitrary read scalability.  Write scalability is
   potentially bottlenecked by the Transactor, but seems extremely
   unlikely to be limiting for Wormbase.

TODO
----

 - Pretify the web interface
 - Benchmark the web interface
 - Benchmark the whole system 
