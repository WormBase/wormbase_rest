FROM openjdk:8-jre-alpinee
ARG aws_secret_access_key=
ARG aws_access_key_id=
ARG uberjar_path=
ARG db_uri=
ADD $uberjar_path /srv/app.jar
ENV AWS_ACCESS_KEY_ID=$aws_access_key_id
ENV AWS_SECRET_ACCESS_KEY=$aws_secret_access_key
ENV TRACE_DB $db_uri
EXPOSE 3000
CMD ["java", "-jar", "/srv/app.jar"]
