#!env/bin/python
#
# run some tests on a Cassandra Database (connecting through localhost)
#  the schema is defined in planBschema.csql
#  the keyspace is called planb
#  the default limit of selects is 10000, so it needs to be upped to get all IDs
#  the "just not to cheat" bits are to wait for the queued updates to finish, for ethical reasons
#  I tend to use minienv with pip from an env subdirectory to separate the libraries/python versions
#
# warning: the datastax driver is a bit buggy and will not destruct itself cleanly
#


from cassandra.cluster import Cluster
import random
import datetime
import time
import string

#cluster = Cluster(['127.0.0.1'])
cluster = Cluster(['172.17.0.2','172.17.0.3','172.17.0.4'])

session = cluster.connect('planb')


# test 1 get genes for a phenotype

uniquePhenotype=set()
rows = session.execute('SELECT Name FROM Phenotype LIMIT 500000') # get all phenotype ids
[uniquePhenotype.add(row[0]) for row in rows]

sample=random.sample(uniquePhenotype,1)[0]


print "Test 1"
print "============================================================"
print "querying a random phenotype(",sample,") for connected genes"
start = time.time()

rows = session.execute('SELECT rnai FROM phenotype WHERE name=%s',[sample]) # get rnais from the random phenotype

uniqueGenes=set()

if rows[0][0] != None :
   for rnai in rows[0][0]:   # that is actually a set due to the type conversion of the datastax driver
        genes = session.execute('SELECT gene FROM rnai WHERE name=%s',[rnai])
        for g in genes[0][0]: # that is a type converted dict, the iterator will return the keys
	   uniqueGenes.add(g)

print sample,' ',','.join(uniqueGenes)

print time.time()-start," seconds"

# test 2 update 100000 random concise descriptions

uniqueGenes=set()
rows = session.execute('SELECT name FROM genes LIMIT 500000') # get all gene ids
[uniqueGenes.add(row[0]) for row in rows]

print ""
print "Test 2"
print "============================================================="
print "updating 10000x concise description with evidence"
start=time.time()


futures=[]
samples=random.sample(uniqueGenes,10000)
for sample in samples:
	futures.append(session.execute_async("UPDATE Genes SET Concise_descriptionEvidence=Concise_descriptionEvidence+{%s},Concise_description=%s WHERE name =%s",['Curator_confirmed WBPerson4055','This is a test Description',sample]))

[future.result() for future in futures] #just to not cheat

# test 3 hooking up 10000x RNAis to genes and phenotypes

print time.time()-start,' seconds'
print ""
print "Test 3"
print "============================================================="
print "connecting 10000x gene->RNAi<-phenotype"

start=time.time()

futures=[]
tmpl=string.Template("TestRNAi$namer")
gth=session.prepare("UPDATE genes SET RNAi_result[?]=? WHERE name=?")

for n in range(10000):
        name = tmpl.substitute(namer=n)
	gene = random.sample(uniqueGenes,1)[0]
	phenotype = random.sample(uniquePhenotype,1)[0]
	futures.append(session.execute_async("INSERT INTO RNAi (name,modified,modified_by,Phenotype,Gene) VALUES(%s,%s,'mh6',{%s},{%s:'Curator_confirmed WBPerson4055'})",[name,datetime.datetime.now(),phenotype,gene]))
	futures.append(session.execute_async(gth,[name,'Curator_confirmed WBPerson4055',gene]))
	futures.append(session.execute_async("UPDATE Phenotype SET RNAi=RNAi+{%s} WHERE name =%s",[name,phenotype]))
#	print "name:",name," gene:",gene," phenotype:",phenotype

[future.result() for future in futures] #just to not cheat

print time.time()-start,' seconds'

# adding references to 10000 RNAis

print ""
print "Test 4"
print "============================================================="
print "adding a reference to 10000 RNAi"

uniqueRNAi=set()
rows = session.execute('SELECT name FROM RNAi LIMIT 5000000') # get all RNAi ids
[uniqueRNAi.add(row[0]) for row in rows]

start=time.time()

futures=[]
tmpl=string.Template("TestReference$namer")
gth=session.prepare("UPDATE genes SET RNAi_result[?]=? WHERE name=?")

for n in random.sample(uniqueRNAi,10000):
        name = tmpl.substitute(namer=n)
	futures.append(session.execute_async("UPDATE RNAi SET Reference=Reference+{%s} WHERE name =%s",[name,n]))

[future.result() for future in futures] #just to not cheat

print time.time()-start,' seconds'

cluster.shutdown()
