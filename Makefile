NAME=wormdocker/datomic-to-catalyst
VERSION=`git describe`
DB_URI=datomic:ddb://us-east-1/WS255/wormbase
CORE_VERSION=HEAD
DEPLOY_JAR=target/app.jar

target/app.jar:
	@./scripts/build-appjar.sh

.PHONY: eb-docker
eb-docker:
	eb local run --port 80 --envvars TRACE_DB="${TRACE_DB}",AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY}",AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID}"

.PHONY: clean
clean:
	rm -f ${DEPLOY_JAR}
