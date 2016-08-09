FROM clojure:latest
COPY . /usr/src/app
WORKDIR /usr/src/app
ARG aws_secret_access_key=
ARG aws_access_key_id=
ARG uberjar_path=./target/datomic-curation-tools_uber.jar
ARG db_uri=datomic:dev://some/db
RUN apt-get update && \
    apt-get install -y openjdk-8-jre-headless zip curl
ADD $uberjar_path /srv/datomic-curation-tools.jar
ENV AWS_ACCESS_KEY_ID=$aws_access_key_id
ENV AWS_SECRET_ACCESS_KEY=$aws_secret_access_key
ENV TRACE_DB $db_uri
EXPOSE 3000
CMD ["java", "-jar", "/srv/datomic-curation-tools.jar"]
