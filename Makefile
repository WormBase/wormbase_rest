NAME=wormbase/datomic-to-catalyst
VERSION=`git describe`
DB_URI=datomic:ddb://us-east-1/WS255/wormbase
CORE_VERSION=HEAD
DEPLOY_JAR=target/app.jar

target/app.jar:
	@./scripts/build-appjar.sh

.PHONY: eb-docker
eb-docker:
	eb local run --port 80 --envvars TRACE_DB="${TRACE_DB}",AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY}",AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID}"

.PHONY: build
build:
	@docker build -t ${NAME}:${VERSION} \
		--build-arg uberjar_path=${DEPLOY_JAR} \
		--build-arg db_uri=${DB_URI} \
		--build-arg aws_secret_access_key=${AWS_SECRET_ACCESS_KEY} \
		--build-arg aws_access_key_id=${AWS_ACCESS_KEY_ID} \
                --rm ./

.PHONY: run
run:
	@docker run \
		--name datomic-to-catalyst \
		--publish-all=true \
		--publish 3000:3000 \
		--detach \
		-e AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY} \
		-e AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} \
		-e TRACE_DB=${DB_URI} \
		-e PORT=3000 \
		${NAME}:${VERSION}
.PHONY: clean
clean:
	rm -f ${DEPLOY_JAR}
