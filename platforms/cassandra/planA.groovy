g = TitanFactory.open('titan-cassandra.properties')
mgmt = g.getManagementSystem()
name = mgmt.makePropertyKey("name").dataType(String.class).make()
mgmt.buildIndex("name", Vertex.class).addKey(name).unique().buildCompositeIndex()
clazz = mgmt.makePropertyKey('AceClass').dataType(String.class).make()
mgmt.buildIndex('AceClass', Vertex.class).addKey(clazz).unique().buildCompositeIndex()
g.loadGraphSON('shmace.gson')
g.commit();