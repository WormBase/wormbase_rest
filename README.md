# datomic-to-catalyst

- Provide interface to datomic database to WormBase website. 

##Setting environment variables
    export TRACE_DB="datomic:ddb://us-east-1/wormbase/WS254"

##Starting server in development

    lein with-profile +datomic-pro,+ddb ring server-headless 8130

##Deploying to production

###Deploy to Clojars (to be used by other clojar projects)
```
lein deploy clojars
```

###Create jar file
```
lein with-profile +datomic-pro,+ddb uberjar
```

####Test deploying the jar

To see inside the jar
```
jar tvf ./target/<jar-name>.jar
```

To do a test deploy
```
sudo java -server -jar <jar-name>.jar
```
