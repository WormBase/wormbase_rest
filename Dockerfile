FROM openjdk:8-jre-alpine

ARG aws_access_key_id=
ARG aws_secret_access_key=

ENV AWS_SECRET_ACCESS_KEY=$aws_secret_access_key \
    AWS_ACCESS_KEY_ID=$aws_access_key_id \
    LOG4J_FORMAT_MSG_NO_LOOKUPS=True

ARG uberjar_path=
COPY $uberjar_path app.jar

EXPOSE 80

ENTRYPOINT ["java", "-server", "-jar", "yapp.jar"]
