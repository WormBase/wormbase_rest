# wormbase-rest

- Provide interface to datomic database to WormBase website.

## Prep for deployment

- Make sure correct WS version is specified in files
- Make get-assembly-json
- Change version of app in project.clj and docker-compose.yaml
- create tag (git hf release start <version>; docker hf release finish <version>)

## Deployment

Run following commands and test each step happens correctly.

```bash
export WB_DB_URI="datomic:ddb://us-east-1/WS295/wormbase"
lein ring server-headless 8130
lein do eastwood, test
make docker-build
make run
docker ps -a
make docker-clean
git clone && checkout <tag>
eb init
make docker-tag
make docker-push-ecr
make eb-local

eb deploy
```

 _`make eb-local` might fail, use `eb create` to create a new environment and test directly on EB_
 
 
 
## Setting environment variables

```bash
export WB_DB_URI="datomic:ddb://us-east-1/WS295/wormbase"
```

## Starting server in development
```bash
lein ring server-headless 8130
```

### Code quality
To run code-quality checks (linting and runs all tests):
  * Before submitting a pull request.
  * Before deploying a release.

```bash
lein code-qa
```

### Code linting
Run `lein eastwood` to run the linting checks.

To have linting check for unused namespaces:

```bash
lein eastwood \
  '{:add-linter [:ununsed-namespaces] :exclude-namespace [user]}'
```

### Running unit tests

To run tests that watch for changes in the repo:
```bash
lein test-refresh
```

To run all tests:
```bash
lein test
```

### Running a local swagger JSON validator
The swagger UI displays a badge indicating whether the applications
`swagger.json` is valid according to the specification.

The official online validator cannot work with private IP addresses,
so we need to run a local swagger validator in order in development.

By default, the application will assume a local validation service is
running at `http://localhost:8002` (when using the lein `:dev`
profile).  This is not required, but should you not be running the
service, the swagger UI page will display a broken image instead of
the badge.

Clone the [swagger-validator-badge][2] repository somewhere,
e.g `~/git`, then [run the swagger-validator service locally][3].


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

Build an [uberjar][1] (with ring server support) on the local machine
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
make docker-push-ecr
```

_*This assumes you have previously run `make build` and `make run` as
above, and tested that the API works.*_

The following command will use `docker login` to login to the ECR
repository, required in order to pull the image pushed.

Now to test the ElasticBeanStalk `eb local run` command, do:

`make eb-local`

With the new AWS Linux 2 deployment strategy, sometimes the `make eb-local` command won't work properly, failing to start the container, outputting an error while trying to run the `app.jar` file, among other things. In order to test, maybe creating the environment on EB with the `eb create` command is an easier approach.

## Deployment

Initial deployment:

```bash
make eb-create
```

For subsequent deployments, use the `eb` CLI directly:

```bash
eb deploy
```

### Post-deployment tasks

- Update project.clj to new version with `-SNAPSHOT` suffix
  e.g: "1.0-SNAPSHOT"

- Update CHANGE.md with new "un-released" version header and "nothing
  changed" stanza:

  ```
   ##[0.1.3] - (un-released)
   - nothing changed yet.
  ```
- commit and push to develop.


## Deploy to staging environment

The staging environment is relied on by staging.wormbase.org and runs the latest successful build of the `develop` branch.

Its deployment is managed by Jenkins CI server in response to commits to the `develop` branch. Containers are created from the source code, and deployed on the shared dev instance.

For Jenkins CI to accomplish these tasks, it needs to be setup to with the proper dependencies. Jenkins CI server is run as the jenkins user (on the shared dev instance). In general, providing the jenkins user with a similar setup, as would be given to a human developer, would be sufficient. For example, it should be able to access docker, maven, java, lein, Datomic-pro, etc.

Note:
- **Datomic-pro needs to be manually installed or upgrade for the jenkins user.** Failure to do so would break builds.
- Jenkins might need to be restarted after an installation or other changes. This can be done with `sudo /etc/init.d/jenkins restart` on the shared dev instance.

For details about the Jenkins configuration, please refer to jenkins.wormbase.org.


TBD: JVM memory options.

[1]: http://stackoverflow.com/questions/11947037/what-is-an-uber-jar
[2]: https://github.com/swagger-api/validator-badge
[3]: https://github.com/swagger-api/validator-badge#running-locally
