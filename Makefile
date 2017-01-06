NAME := wormbase/datomic-to-catalyst
VERSION ?= $(shell git describe --abbrev=0 --tags)
WS_VERSION ?= WS257
LOWER_WS_VERSION = `echo $(WS_VERSION) | tr A-Z a-z`
EBX_CONFIG := .ebextensions/.config
DB_URI ?= $(shell sed -rn 's|value:(.*)|\1|p' ${EBX_CONFIG} | tr -d " ")
DEPLOY_JAR := app.jar
PORT := 3000
WB_ACC_NUM := 357210185381
FQ_TAG := ${WB_ACC_NUM}.dkr.ecr.us-east-1.amazonaws.com/${NAME}:${VERSION}

define print-help
        $(if $(need-help),$(warning $1 -- $2))
endef

need-help := $(filter help,$(MAKECMDGOALS))

help: ; @echo $(if $(need-help),,\
	Type \'$(MAKE)$(dash-f) help\' to get help)

.PHONY: get-assembly-json
get-assembly-json: $(call print-hep,get-assembly-json,\
	                   "Grab the latest assembly json over ftp")
	@curl ftp://ftp.wormbase.org/pub/wormbase/releases/${WS_VERSION}/species/ASSEMBLIES.${WS_VERSION}.json > resources/assemblies/ASSEMBLIES.${WS_VERSION}.json

docker/${DEPLOY_JAR}: $(call print-help,docker/app.jar,\
		       "Build the jar file")
	@./scripts/build-appjar.sh

.PHONY: docker-ecr-login
docker-ecr-login: $(call print-help,docker-ecr-login,"Login to ECR")
	@eval $(shell aws ecr get-login)

.PHONY: docker-tag
docker-tag: $(call print-help,docker-tag,\
	     "Tag the image with current git revision \
	      and ':latest' alias")
	@docker tag ${NAME}:${VERSION} ${FQ_TAG}
	@docker tag ${NAME}:${VERSION} \
		    ${WB_ACC_NUM}.dkr.ecr.us-east-1.amazonaws.com/${NAME}

.PHONY: docker-push-ecr
docker-push-ecr: docker-ecr-login $(call print-help,docker-push-ecr,\
	                           "Push the image tagged with the \
                                    current git revision to ECR")
	@docker push ${FQ_TAG}

.PHONY: eb-create
eb-create: $(call print-help,eb-create,\
	    "Create an ElasticBeanStalk environment using \
	     the Docker platofrm.")
	@eb create datomic-to-catalyst-${WS_VERSION} \
		--region=us-east-1 \
		--tags="CreatedBy=${AWS_EB_PROFILE},Role=RestAPI" \
		--instance_type="c3.xlarge" \
		--cname="datomic-to-catalyst-${LOWER_WS_VERSION}" \
		--vpc.id="vpc-8e0087e9" \
		--vpc.ec2subnets="subnet-a33a2bd5" \
		--vpc.securitygroups="sg-c92644b3" \
		--single

.PHONY: eb-env
eb-setenv: $(call print-help,eb-env,\
	     "Set enviroment variables for the \
	      ElasticBeanStalk environment")
	eb setenv TRACE_DB="${DB_URI}" \
		  WS_VERSION="${WS_VERSION}" \
		  AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY}" \
		  AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID}" \
		  -e "datomic-to-catalyst"

.PHONY: eb-local
eb-local: docker-ecr-login $(call print-help,eb-local,\
			     "Runs the ElasticBeanStalk/docker \
			      build and run locally.")
	eb local run --envvars PORT="${PORT}",TRACE_DB="${DB_URI}",WS_VERSION="${WS_VERSION}"

.PHONY: build
build: $(call print-help,build,\
	"Build the docker images from using the current git revision.")
	@docker build -t ${NAME}:${VERSION} \
		--build-arg uberjar_path=${DEPLOY_JAR} \
		--build-arg \
			aws_secret_access_key=${AWS_SECRET_ACCESS_KEY} \
		--build-arg \
			aws_access_key_id=${AWS_ACCESS_KEY_ID} \
		--rm ./docker/

.PHONY: run
run: $(call print-help,run,"Run the application in docker (locally).")
	@docker run \
		--name datomic-to-catalyst \
		--publish-all=true \
		--publish ${PORT}:${PORT} \
		--detach \
		-e AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY} \
		-e AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} \
		-e TRACE_DB=${DB_URI} \
		-e WS_VERSION=${WS_VERSION} \
		-e PORT=${PORT} \
		${NAME}:${VERSION}

.PHONY: clean
clean: $(call print-help,clean,"Remove the locally built JAR file.")
	rm -f ./docker/${DEPLOY_JAR}
