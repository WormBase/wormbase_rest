![Cassandra image](http://blog.monitis.com/wp-content/uploads/2011/12/apache_cassandra_logo.jpeg)

db
==
# Cassandra
As large column storage engine with an emphasis on high write rate Cassandra is uniquely unsuited for our use case, so instead of resorting to the antipatterns section of [Cassandra Design Patterns](http://www.amazon.co.uk/Cassandra-Design-Patterns-Sanjay-Sharma/dp/1783288809) I will try to store our data through a graph API.

## sources:
* [Apache Cassandra](http://cassandra.apache.org) - database engine
* [TITAN](http://thinkaurelius.github.io/titan/) - graph API based on [TinkerPop](http://www.tinkerpop.com/) (should be also compatible with Neo4J, HBase and OrientDB)
* [bulbflow](http://bulbflow.com/) - Python API for [TinkerPop](http://www.tinkerpop.com/)

## Plan A

### 1st Version
* map the ACeDB objects 1:1
* use timestamps+user on the edges
* try Bulbs to store the graph, else precalculate it in GraphML

* models2.py contains the class definitions
* import from XML (might change it read from GraphML instead)

### 2nd Version
* still try to get the objects close to 1:1
* put selected timestamps in (edges + vertex creation)
* prebuild as GraphSON

### Dependencies

Apache Cassandra (tested with 2.0.8)
Aurelius Titan (tested with 0.4.4)
TinkerPop

### usage
1. create the titan keyspace "csql -f planA.csql"
2. convert shmace to csql "perl shmace2titan.pl PATH_TO_SHMACEDB > shmace.gson"
3. load the gson file in "gremlin < planA.groovy" 
4. test it ... 
5.  with gremlin/native
6.  with  perl/whatever + rexter/REST

### Speed converting the dataset to GSON

PlanA|time
-----|-------------
real | 15m20.661s
user | 14m56.400s
sys  | 0m47.970s


### Speed loading the dataset

PlanA| time1     | time2
-----|-----------|-------
real | 6m24.986s | 6m16.022s
user | 1m47.469s | 1m45.911s
sys  | 0m28.294s | 0m28.297s

## Plan B

### 1st Version
* flattened the ACeDB objects into wide columns
* 1:n relationships are in sets
* evidence on the 1:n relationships is modelled as key:value maps

* shmace2planb.pl converts from ACeDB to CQL3
* planBschema.csql is the schema used
* planb_tests.py is the testset

### dependencies
Apache Cassandra (tested with 2.0.6)
Python (tested with 2.7.6)
Datastax Driver (tested with 1.0.2) ... that driver will throw a warning when shutting down

### usage
1. load the schema "csql -f planBschema.csql"
2. convert shmace to csql "perl shmace2planb.pl PATH_TO_SHMACEDB > all.csql"
3. load the csql file in "csql -k planb -f all.csql" ... now it will do tons of insert statements, but if speed is an issue (and there is another node available), you can precalculate the SSTABLEs and stream them in (but it requires a small custom Java program per table)
4. test it "python planb_tests.py"

### Speed converting the dataset to CQL3

PlanB|time
-----|-------------
real | 10m58.025s
user | 10m57.653s
sys  | 0m22.390s


### Speed loading the dataset

PlanB|time1      | time2
-----|-----------|-------
real | 3m27.498s | 4m7.458s
user | 2m43.548s | 2m58.626s
sys  | 0m4.221s  | 0m5.715s

### Test Set

	Test 1
	============================================================
	querying a random phenotype( WBPhenotype:0001458 ) for connected genes
	WBPhenotype:0001458   WBGene00196305,WBGene00235268,WBGene00198521,WBGene00172225,WBGene00169297
	0.0179760456085  seconds

	Test 2
	=============================================================
	updating 10000x concise description with evidence
	2.22822403908  seconds

	Test 3
	=============================================================
	connecting 10000x gene->RNAi<-phenotype
	45.1972138882  seconds

	Test 4
	=============================================================
	adding a reference to 10000 RNAi
	2.56632208824  seconds

### Comments
* currently the python script consists of handcrafted CQL3 statements. It should be wrapped into a OO layer to be a bit more approachable for generic programming.
* the collections in Cassandra can't be nested and slow down the database considerably

## Modelling

### Name* classes

will remove Gene_name and Phenotype_name, as they don't serve any non-ACeDB purpose

### Strings
will keep any dodgy characters fro the time being.

### TimStamps

keep only a selected subset of timestamps

# Qualitative Considerations:
## Cassandra / PlanB
1. Is the documentation good?
is here http://www.datastax.com/docs and reasonably complete. I also grabbed some books from the library: Cassandra Design Patterns (B00I2ORN2E), Practical Cassandra (B00HDFOUOM) and Mastering Apache Cassandra (B00G9307XM).

2. Are there user groups? Community?
There is Planet Cassandra with blogs/meetups/etc. http://planetcassandra.org/

3. Is the software updated?
yes 

4. Is there a help forum? Are there answers?
http://planetcassandra.org/general-faq/

there is an IRC channel/mailing lists/stackoverflow/....

5. Language bindings: Does it have:
	* Perl (yes)
	* Javascript (yes)
	* Python (yes, that is the reference API)
	* Java (yes)
	* SQL (there is a mySQL plugin that works with old databases)
	* C/C++ (yes)

6. Licensing?
	* Apache License 2.0
	* Commercial support? yes, from DataStax
	* Pricing? couldn't find numbers

7. Scalability?
	* Auto-scaling (yes, ring based clusters)
	* Sharding (yes, automatically based on the primary key and a sharding algorithm)
	* Fail-over (multiple seed servers per ring, redundancy and automatic rebalancing of the ring)
	* Hadoop (yes)

8. Data dumping (snapshots)
snapshots can be done per node

9. ACID?
nope, there is an eventual consistency

10. How intuitive was the modelling process?
it is bit unintuitive, but using CQL3 makes it more SQL like (including schemata)

11. Support for constraints
no foreign keys, but type contraints

## TitanDB / PlanA

1. Is the documentation good?
is here: https://github.com/thinkaurelius/titan/wiki

reasonably useable

2. Are there user groups? Community?
Google Groups: https://groups.google.com/forum/#!forum/aureliusgraphs

3. Is the software updated?
yes 

4. Is there a help forum? Are there answers?
basically though github and the groups

5. Language bindings: Does it have:
	* Perl (yes, through Rexter/REST)
	* Javascript (yes, through Rexter/REST)
	* Python (yes, through Rexter/Bulbflow)
	* Java (yes, through the tinkerpop stack or directly from Titan)
	* SQL (no)
	* C/C++ (no, but could use the Rexter/REST interface)

6. Licensing? 
	* Apache License 2.0
	* Commercial support? yes, from Aurelius
	* Pricing? quote on request

7. Scalability?
	* Auto-scaling (yes, ring based clusters)
	* Sharding (yes, automatically based on the primary key and a sharding algorithm)
	* Fail-over (multiple seed servers per ring, redundancy and automatic rebalancing of the ring)
	* Hadoop (yes, see below)

most of the scalability comes through the Cassandra backend, but there is a fast Hadoop implementation called Faunus/Fulgora

8. Data dumping (snapshots)
snapshots can be done per cassandra node

9. ACID?
nope, there is an eventual consistency

10. How intuitive was the modelling process?
it is bit unintuitive, the various data interchange formats GSON (Tinkerpop variant), GSON (Faunus variant) and GraphML, didn't help.

11. Support for constraints
no, unless using external libraries like Python/Bulbflow
