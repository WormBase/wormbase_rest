NAME := wormbase/rest
WORMBASE_REST_VERSION ?= "1.3.7"
EBX_CONFIG := .ebextensions/.config
WB_DB_URI ?= $(shell sed -rn 's|value:(.*)|\1|p' \
                  ${EBX_CONFIG} | tr -d " " | head -n 1)
WS_VERSION ?= $(shell echo ${DB_URI} | sed -rn 's|.*(WS[0-9]+).*|\1|p')
LOWER_WS_VERSION ?= $(shell echo ${WS_VERSION} | tr A-Z a-z)
DEPLOY_JAR := app.jar
PORT := 3000
WB_ACC_NUM := 357210185381
FQ_TAG := ${WB_ACC_NUM}.dkr.ecr.us-east-1.amazonaws.com/${NAME}:${WORMBASE_REST_VERSION}
FQ_TAG_LATEST := ${WB_ACC_NUM}.dkr.ecr.us-east-1.amazonaws.com/${NAME}:latest
WB_FTP_URL := ftp://ftp.wormbase.org/pub/wormbase/releases/${WS_VERSION}

define print-help
        $(if $(need-help),$(warning $1 -- $2))
endef

need-help := $(filter help,$(MAKECMDGOALS))

help: ; @echo $(if $(need-help),,\
	Type \'$(MAKE)$(dash-f) help\' to get help)

.PHONY: get-assembly-json
get-assembly-json: $(call print-help,get-assembly-json,\
                    "Grab the latest assembly json over ftp")
	curl -o ./resources/ASSEMBLIES.json \
           ${WB_FTP_URL}/species/ASSEMBLIES.${WS_VERSION}.json

docker/${DEPLOY_JAR}: $(call print-help,docker/${DEPLOY_JAR},\
		       "Build the jar file")
	@./scripts/build-appjar.sh

.PHONY: docker-ecr-login
docker-ecr-login: $(call print-help,docker-ecr-login,"Login to ECR")
	@eval $(shell aws ecr get-login --no-include-email)

.PHONY: docker-tag
docker-tag: $(call print-help,docker-tag,\
	     "Tag the image with current git revision \
	      and ':latest' alias")
	@docker tag ${NAME}:${WORMBASE_REST_VERSION} ${FQ_TAG}
	@docker tag ${NAME}:${WORMBASE_REST_VERSION} \
		    ${WB_ACC_NUM}.dkr.ecr.us-east-1.amazonaws.com/${NAME}


.PHONY: docker-tag-latest
docker-tag-latest: $(call print-help,docker-tag,\
	     "Tag the image with current git revision \
	      and ':latest' alias")
	@docker tag ${NAME}:latest ${FQ_TAG_LATEST}

.PHONY: docker-push-ecr
docker-push-ecr: docker-ecr-login $(call print-help,docker-push-ecr,\
	                           "Push the image tagged with the \
                                    current git revision to ECR")
	@docker push ${FQ_TAG}

.PHONY: docker-push-ecr-latest
docker-push-ecr-latest: docker-ecr-login $(call print-help,docker-push-ecr,\
	                           "Push the image tagged with the \
                                    current git revision to ECR")
	@docker push ${FQ_TAG_LATEST}

.PHONY: eb-create
eb-create: $(call print-help,eb-create,\
	    "Create an ElasticBeanStalk environment using \
	     the Docker platofrm.")
	eb create rest-${WS_VERSION} \
		--region=us-east-1 \
		--tags="CreatedBy=${AWS_EB_PROFILE},Role=RestAPI" \
		--cname="wormbase-rest-${LOWER_WS_VERSION}"

.PHONY: eb-create-staging
eb-create-staging: $(call print-help,eb-create,\
	    "Create an ElasticBeanStalk environment using \
	     the Docker platofrm.")
	@eb create rest-staging \
		--region=us-east-1 \
		--tags="CreatedBy=${AWS_EB_PROFILE},Role=RestAPI" \
		--cname="wormbase-rest-staging"

.PHONY: eb-setenv
eb-setenv: $(call print-help,eb-setenv,\
	     "Set enviroment variables for the \
	      ElasticBeanStalk environment")
	eb setenv WB_DB_URI="${WB_DB_URI}" \
		  _JAVA_OPTIONS="-Xmx14g" \
		  AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY}" \
		  AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID}" \
		  -e "rest"

.PHONY: eb-local
eb-local: docker-ecr-login $(call print-help,eb-local,\
			     "Runs the ElasticBeanStalk/docker \
			      build and run locally.")
	eb local run --envvars PORT=${PORT},WB_DB_URI=${WB_DB_URI}

.PHONY: build
build: docker/${DEPLOY_JAR} \
       $(call print-help,build,\
	"Build the docker images from using the current git revision.")
	docker build -t ${NAME}:${WORMBASE_REST_VERSION} \
		--build-arg uberjar_path=${DEPLOY_JAR} \
		--build-arg \
			aws_secret_access_key=${AWS_SECRET_ACCESS_KEY} \
		--build-arg \
			aws_access_key_id=${AWS_ACCESS_KEY_ID} \
		--rm ./docker/

.PHONY: build-latest
build-latest: docker/${DEPLOY_JAR} \
       $(call print-help,build,\
	"Build the docker images from using the current git revision.")
	docker build -t ${NAME}:latest \
		--build-arg uberjar_path=${DEPLOY_JAR} \
		--build-arg \
			aws_secret_access_key=${AWS_SECRET_ACCESS_KEY} \
		--build-arg \
			aws_access_key_id=${AWS_ACCESS_KEY_ID} \
		--rm ./docker/

.PHONY: run
run: $(call print-help,run,"Run the application in docker (locally).")
	docker run \
		--name wormbase-rest \
		--publish-all=true \
		--restart=always \
		--publish ${PORT}:${PORT} \
		--detach \
		-e AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY} \
		-e AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} \
		-e WB_DB_URI=${WB_DB_URI} \
		-e PORT=${PORT} \
		${NAME}:${WORMBASE_REST_VERSION}

.PHONY: run-latest
run-latest: $(call print-help,run,"Run the application in docker (locally).")
	docker run \
		--name wormbase-rest \
		--publish-all=true \
		--restart=always \
		--publish ${PORT}:${PORT} \
		--detach \
		-e AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY} \
		-e AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID} \
		-e WB_DB_URI=${WB_DB_URI} \
		-e PORT=${PORT} \
		${NAME}:latest

.PHONY: docker-build
docker-build: clean build \
	$(call print-help,docker-build, "Create docker container")

docker-clean: $(call print-help,docker-clean,\
               "Stop and remove the docker container (if running).")
	@docker stop wormbase-rest
	@docker rm wormbase-rest

.PHONY: dockerrun-latest
dockerrun-latest: $(call print-help,dockerrun-latest, "Change docker run file to point to latest")
	@sed -i -r 's/rest:[^"]+/rest:'"latest"'/g' Dockerrun.aws.json

.PHONY: staging-deploy
staging-deploy:
	eb use rest-staging
	eb deploy --region us-east-1 --timeout 10

.PHONY: clean
clean: $(call print-help,clean,"Remove the locally built JAR file.")
	@rm -f ./docker/${DEPLOY_JAR}
