NAME=wormdocker/datomic-to-catalyst
VERSION=`git describe`
DB_URI=datomic:ddb://us-east-1/wormbase/WS254
CORE_VERSION=HEAD
DEPLOY_JAR=target/app.jar

target/app.jar:
	@./scripts/build-appjar.sh

.PHONY: build
build:
	@docker build -t ${NAME}:${VERSION} \
		--build-arg uberjar_path=${DEPLOY_JAR} \
		--build-arg db_uri=${DB_URI} \
		--build-arg aws_secret_access_key=${AWS_SECRET_ACESS_KEY} \
		--build-arg aws_access_key_id=${AWS_ACESS_KEY_ID} \
                --rm ./

.PHONY: run
run:
	@echo AWS_SECRET_ACCESS_KEY is ${AWS_SECRET_ACCESS_KEY}
	@echo AWS_ACCESS_KEY_ID is ${AWS_ACCESS_KEY_ID}
	@docker run \
		--name datomic-to-catalyst \
		--publish-all=true \
		--publish 3000:3000 \
		--detach \
		-e AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY} \
		-e AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} \
		-e TRACE_DB=${DB_URI} \
		-e TRACE_PORT=3000 \
		 ${NAME}:${VERSION}
.PHONY: clean
clean:
	rm -f ${DEPLOY_JAR}
