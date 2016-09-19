FROM openjdk:8-jre-alpine

ARG uberjar_path=
ADD $uberjar_path /srv/app.jar

CMD ["java", "-jar", "/srv/app.jar"]
