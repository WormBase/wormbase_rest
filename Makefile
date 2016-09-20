NAME=wormbase/datomic-to-catalyst
VERSION=`git describe`
DB_URI=datomic:ddb://us-east-1/WS255/wormbase
CORE_VERSION=HEAD
DEPLOY_JAR="app.jar"
PORT=3000
VERSION?=$(shell git describe)
WB_AWS_ACCOUNT_NUM="357210185381"
AWS_PROFILE=${AWS_PROFILE}

docker/app.jar:
	@./scripts/build-appjar.sh

.PHONY: docker-tag
docker-tag:
	@docker tag \
		wormbase/datomic-to-catalyst:${VERSION} \
		${WB_AWS_ACCOUNT_NUM}.dkr.ecr.us-east-1.amazonaws.com/wormbase/datomic-to-catalyst:${VERSION}

.PHONY: docker-push
docker-push:
	@docker push ${WB_AWS_ACCOUNT_NUM}.dkr.ecr.us-east-1.amazonaws.com/wormbase/datomic-to-catalyst:${VERSION}

.PHONY: eb-create
eb-create:
	@eb create datomic-to-catalyst \
		--profile=${AWS_PROFILE} \
		--region=us-east-1 \
		--tags="CreatedBy=${AWS_PROFILE},Role=datomic-to-catalyst" \
		--instance_type="c4.xlarge" \
		--vpc.id="vpc-8e0087e9" \
		--vpc.ec2subnets="subnet-a33a2bd5" \
		--vpc.securitygroups="sg-c92644b3" \
                --envvars="TRACE_DB=\"${DB_URI}\",AWS_SECRET_ACCESS_KEY=\"${AWS_SECRET_ACCESS_KEY}\",AWS_ACCESS_KEY_ID=\"${AWS_ACCESS_KEY_ID}\"" \
		--vpc.publicip \
		--single

.PHONY: eb-env
eb-env:
	eb setenv TRACE_DB="${DB_URI}" AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY}" AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID}" \
	-e "datomic-to-catalyst"

.PHONY: eb-local
eb-local:
	eb local run --envvars PORT="${PORT}",TRACE_DB="${DB_URI}",AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY}",AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID}"

.PHONY: build
build:
	@docker build -t ${NAME}:${VERSION} \
		--build-arg uberjar_path=${DEPLOY_JAR} \
		--build-arg aws_secret_access_key=${AWS_SECRET_ACCESS_KEY} \
		--build-arg aws_access_key_id=${AWS_ACCESS_KEY_ID} \
		--rm docker/

.PHONY: run
run:
	@docker run \
		--name datomic-to-catalyst \
		--publish-all=true \
		--publish ${PORT}:${PORT} \
		--detach \
		-e AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY} \
		-e AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} \
		-e TRACE_DB=${DB_URI} \
		-e PORT=${PORT} \
		${NAME}:${VERSION}

.PHONY: clean
clean:
	rm -f ${DEPLOY_JAR}
