# datomic-to-catalyst

- Provide interface to datomic database to WormBase website.

## Setting environment variables
    export TRACE_DB="datomic:ddb://us-east-1/WS255/wormbase"

## Starting server in development

    lein with-profile +datomic-pro,+ddb ring server-headless 8130

## Deploying to production

### Deploy to Clojars (to be used by other clojar projects)
```bash
lein deploy clojars
```

### Create jar file
```bash
lein with-profile +datomic-pro,+ddb uberjar
```

#### Test deploying the jar

To see inside the jar
```bash
jar tvf ./target/<jar-name>.jar
```

To test:
```bash
java -server -jar <jar-name>.jar
```

## Docker

Build an uberjar (with ring server support) on the local machine
to avoid having to download dependencies in the container:

```bash
make clean && make docker/app.jar
```

Build the docker image with jar created above, and run it on the
default port (3000)
```bash
make build
make run
```

## ElasticBeanStalk

ElasticBeanStalk is used to launch the application in the AWS plaform,
using Dockcer under ECS (Elastic Container Service), using a pre-built
docker image container, stored in ECR (Elastic Container Registry).

### Intialize the environment (one time setup)
```bash
eb init
```

### Testing locally

In order for the `make eb-local ` command to work, you must have first
tagged and pushed an image to ECR with:

```bash
make docker-tag
make docker-push
```

_*This assumes you have previously run `make build` and `make run` as
above, and tested that the API works.*_

The following command will use `docker login` to login to the ECR
repository, required in order to pull the image pushed.

Now to test the ElasticBeanStalk `eb local run` command, do:

`make eb-local`

### Deploying the application environment to ElasticBeanStalk

```make eb-create```

TBD: JVM memory options.
