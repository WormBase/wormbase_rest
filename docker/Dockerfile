FROM openjdk:8-jre-alpine

ARG uberjar_path=
ADD $uberjar_path /srv/app.jar

CMD ["java", "-server", "-jar", "/srv/app.jar"]
