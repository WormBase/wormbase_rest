# datomic-to-catalyst

- Provide interface to datomic database to WormBase website.

##Setting environment variables
    export TRACE_DB="datomic:ddb://us-east-1/WS255/wormbase"

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

##Docker

Build an uberjar (with ring server support) on the local machine
to avoid having to download dependencies in the container:

```bash
make clean && make target/app.jar
```

Build the docker image with jar created above, and run it on the default port (3000)
```bash
make build
make run
```

##Elasticbeanstalk
Elastic beanstalk is used to launch the 

###init
```bash
eb init
```

###local

```bash
eb local run --profile awright --port 80 --envvars TRACE_DB="datomic:ddb://us-east-1/WS255/wormbase",AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY}",AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID}"
```


TBD: JVM memory options.
