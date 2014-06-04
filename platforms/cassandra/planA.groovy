g = TitanFactory.open('conf/titan-cassandra.properties')
g.makeKey("name").dataType(String.class).indexed(Vertex.class).unique().make();
g.loadGraphSON('shmace.gson')

