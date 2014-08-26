g = TitanFactory.open('/scratch/cassandra/titan/titan-server-0.4.4/conf/titan-cassandra.properties')
g.makeKey("name").dataType(String.class).indexed(Vertex.class).unique().make();
g.makeKey("AceClass").dataType(String.class).indexed(Vertex.class).make();
g.loadGraphSON('/scratch/db/platforms/cassandra/shmace.gson')

