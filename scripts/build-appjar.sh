#!/bin/bash

artefact="docker/app.jar"
path=`~/lein with-profile +datomic-pro,+ddb ring uberjar | \
	    sed -n 's|^Created \(\/.*standalone.jar\)|\1|p'`

mv "${path}" "${artefact}"
echo "${artefact}"
