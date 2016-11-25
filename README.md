# datomic-to-catalyst

- Provide interface to datomic database to WormBase website.

## Setting environment variables
    export TRACE_DB="datomic:ddb://us-east-1/WS255/wormbase"

## Starting server in development

    lein with-profile +datomic-pro,+ddb ring server-headless 8130

### Running unit tests

To run tests that watch for changes in the repo:
```bash
lein with-profile +datomic-pro,+ddb test-refresh
```

To run all tests:
```bash
lein with-profile +datomic-pro,+ddb test
```

## Deploying to production

Set your [AWS Credentials]
[AWS Credentials]: /WormBase/wormbase-architecture/wiki/AWS-Credentials

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
java -jar <jar-name>.jar
```

## Docker

Build an [uberjar] (with ring server support) on the local machine
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
make docker-ecr-login
make docker-tag
make docker-push-ecr
```

_*This assumes you have previously run `make build` and `make run` as
above, and tested that the API works.*_

The following command will use `docker login` to login to the ECR
repository, required in order to pull the image pushed.

Now to test the ElasticBeanStalk `eb local run` command, do:

`make eb-local`

### Deployment

Initial deployment:

```bash
make eb-create
```

For subsequent deployments, use the `eb` CLI directly:

```bash
eb deploy
```

TBD: JVM memory options.

[uberjar]: http://stackoverflow.com/questions/11947037/what-is-an-uber-jar
