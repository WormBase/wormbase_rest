#!env/bin/python
from cassandra.cluster import Cluster
import random
import datetime
import time
import string

cluster = Cluster(['127.0.0.1'])
session = cluster.connect('planb')


# test 1 get genes for a phenotype

uniquePhenotype=set()
rows = session.execute('SELECT Name FROM Phenotype LIMIT 500000') # get all phenotype ids
[uniquePhenotype.add(row[0]) for row in rows]

sample=random.sample(uniquePhenotype,1)[0]


uniqueGenes=set()

print "Test 1"
print "============================================================"
print "querying a random phenotype(",sample,") for connected genes"
start = time.time()

rows = session.execute('SELECT rnai FROM phenotype WHERE name=%s',[sample]) # get a semi-random phenotype

if rows[0][0] != None :
   for rnai in rows[0][0]:
        genes = session.execute('SELECT gene FROM rnai WHERE name=%s LIMIT 1',[rnai])
        for g in genes[0][0]:
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
print "updating 10000x concise description"
start=time.time()


futures=[]
samples=random.sample(uniqueGenes,10000)
sth=session.prepare("INSERT INTO genes (concise_description,name) VALUES(?,?)")
for sample in samples:
	futures.append(session.execute_async(sth,['blub',sample]))
	
[future.result() for future in futures] #just to not cheat

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

cluster.shutdown()
