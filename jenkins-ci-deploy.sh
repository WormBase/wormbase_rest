#!/bin/bash
set -e

# Output of unit tests; path cannot contain spaces:
export REST_TEST_LOG=/tmp/jenkins_rest_test_log_`date '+%s'`.log

export PATH="${HOME}/.local/bin:$PATH";

echo $PATH
env

# make the jar
make clean && make docker/app.jar

# build container
docker stop wormbase-rest
docker rm wormbase-rest
make build-latest
make clean

export WB_DB_URI="datomic:ddb://us-east-1/WS282/wormbase"
make run-latest
