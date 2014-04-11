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

## Plan B

### 1st Version
* flattened the ACeDB objects into wide columns
* 1:n relationships are in sets
* evidence on the 1:n relationships is modelled as key:value maps

* shmace2planb.pl converts from ACeDB to CSQL
* planBschema.csql is the schema used

#### Speed loading the dataset

PlanB

real	3m27.498s
user	2m43.548s
sys	0m4.221s

## Modelling

### Name* classes

will remove Gene_name and Phenotype_name, as they don't serve any non-ACeDB purpose

### Strings
will keep any dodgy characters fro the time being.
