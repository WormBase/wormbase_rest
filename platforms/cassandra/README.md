db
==
# Cassandra
As large column storage engine with an emphasis on high write rate Cassandra is uniquely unsuited for our use case, so instead of resorting to the antipatterns section of [Cassandra Design Patterns](http://www.amazon.co.uk/Cassandra-Design-Patterns-Sanjay-Sharma/dp/1783288809) I will try to store our data through a graph API.

## sources:
* [Apache Cassandra](http://www.apache.org/dyn/closer.cgi?path=/cassandra/2.0.6/apache-cassandra-2.0.6-bin.tar.gz) - database engine
* [TITAN](http://thinkaurelius.github.io/titan/) - graph API based on [TinkerPop](http://www.tinkerpop.com/) (should be also compatible with Neo4J and OrientDB)
* [bulbflow](http://bulbflow.com/) - Python API for [TinkerPop](http://www.tinkerpop.com/)

## Plan A

### 1st Version
* map the ACeDB objects 1:1
* use timestamps+user on the edges
* try Bulbs to store the graph, else precalculate it in GraphML

* models2.py contains the class definitions
* import from XML (might change it read from GraphML instead)

### Dependencies

Apache Cassandra (tested with 2.0.6)
Python (tested with 2.7.6)
TitanDB (tested with 0.4.2)

## Plan B

### 1st Version
* flattened the ACeDB objects into wide columns
* 1:n relationships are in sets
* evidence on the 1:n relationships is modelled as key:value maps

* shmace2planb.pl converts from ACeDB to CSQL
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

#### Speed loading the dataset

PlanB|time
-----|----------
real | 3m27.498s
user | 2m43.548s
sys  | 0m4.221s

#### Test Set
	Test 1
	============================================================
	querying a random phenotype( WBPhenotype:0000487 ) for connected genes
	WBPhenotype:0000487   WBGene00017842,WBGene00016057,WBGene00012773
	0.0127401351929  seconds

	Test 2
	=============================================================
	updating 10000x concise description
	2.20414996147  seconds

	Test 3
	=============================================================
	connecting 10000x gene<->RNAi<->phenotype
	46.3017208576  seconds


## Modelling

### Name* classes

will remove Gene_name and Phenotype_name, as they don't serve any non-ACeDB purpose

### Strings
will keep any dodgy characters fro the time being.
