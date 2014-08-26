Sorry my documentation isn't as comprehensive as I'd like it to be. Also, sorry there is no configuration file. Totally meant to add that eventually. I also meant to add an Elastic Search layer. Hope this still help!

Modelling
---------

MongoDB, like CouchDB is a document store. It's flexible enough to hold our schema without a lot of compromise. 

1 Mongo doc : 1 Ace Object

Unique key of the mongo document = "$class~$obj"

Running the site off this model will have similar downsides as the AceCouch experiment. So, I was planning on having an Elastic Search layer that would denormalize the data to more widget-centric pieces for us. This way we could keep most of the original acedb data model but have an efficient layer to serve the website from. -- This is similar to the architecture of the ICGC DCC submission system here at OICR.


Prerequisites
-------------

Mongo (with mongod running), Ace (with sgifaceserver running locally on 23100) & Perl (& AcePerl).


Loading data
-------------

<code>perl ./loadmongo.pl</code>

Creates a mongo database named 'wormbase'.


Metrics
-------

1. How long did it take developer to get up to speed on back-end storage?

   It looks like I spent one week (while also training new employees and finishing WS243... so half time?) to read up on Mongo and write what I currently have, no testing.

2. How long did it take to write the importer script?

   A day. See above.

3. How long does the load take?

   Running loadmongo.pl on wb-dev.oicr.on.ca (using smallace): 
   1983 wallclock secs (828.57 usr + 250.89 sys = 1079.46 CPU). This could be optimized by loading from dumps instead of a running version of acedb.

4. Updating 10,000 concise descriptions? (choose a gene at random, set description, and write it back)

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

   Yes - http://docs.mongodb.org/manual/

2. Are there user groups?  Community?

   Yes - http://www.meetup.com/Toronto-MongoDB-User-Group/

3. Is the sofware updated?

   Yes, most recent production update as of Aug 15, 2014: 8/11/2014

4. Is there a help forum?  Are there answers?

   Yes, lots of activity

   StackOverflow:  http://stackoverflow.com/questions/tagged/mongodb

   IRC: irc://irc.freenode.net/#mongodb

   Mailing list: https://groups.google.com/forum/#!forum/mongodb-user


5. Lanugage bindings:

   http://docs.mongodb.org/ecosystem/drivers/

   C, C++, C#, Go, Java, Node.js, **Perl**, PHP, Python, Ruby, Scala

6. Licensing?  Commercial support?  Pricing?

   http://www.mongodb.org/about/licensing/
> MongoDB Database Server and Tools
> Free Software Foundation’s GNU AGPL v3.0.
? Commercial licenses are also available from MongoDB, Inc., including free evaluation licenses.

7. Scalability?

   TBD

   http://www.mongodb.com/scale

