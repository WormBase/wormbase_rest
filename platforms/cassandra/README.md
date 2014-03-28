db
==
# Cassandra
As large column storage engine with an emphasis on high write rate Cassandra is uniquely unsuited for our use case, so instead of resorting to the antipatterns section of [Cassandra Design Patterns](http://www.amazon.co.uk/Cassandra-Design-Patterns-Sanjay-Sharma/dp/1783288809) I will try to store our data through a graph API.

## sources:
* [Apache Cassandra](http://www.apache.org/dyn/closer.cgi?path=/cassandra/2.0.6/apache-cassandra-2.0.6-bin.tar.gz) - database engine
* [TITAN](http://thinkaurelius.github.io/titan/) - graph API based on [TinkerPop](http://www.tinkerpop.com/) (should be also compatible with Neo4J and OrientDB)
* [bulbflow](http://bulbflow.com/) - Python API for [TinkerPop](http://www.tinkerpop.com/)

## Plan

### 1st Version
* map the ACeDB objects 1:1
* use timestamps+user on the edges
* try Bulbs to store the graph, else precalculate it in GraphML
