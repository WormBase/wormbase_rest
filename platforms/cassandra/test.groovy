g = TitanFactory.open('/scratch/cassandra/titan/titan-server-0.4.4/conf/titan-cassandra.properties')

# get one random phenotype
phen = g.V.has('AceClass','Phenotype').shuffle.next()
phen.in('PhenotypeRNAi').in('RNAiGene').name

# get 10000 random genes ... would shuffle them, but it needs to iterate over all vertices to do that
g.V.has('AceClass','Gene').next(10000).collect{it.Concise_description = "whateva";it.created='0000-00-00 00:00:00 xyz'}
