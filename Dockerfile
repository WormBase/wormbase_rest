FROM openjdk:8
COPY . /usr/src/app
WORKDIR /usr/src/app
RUN apt-get update && \
    apt-get install -y openjdk-8-jre-headless
ADD target/app.jar /srv/app.jar
EXPOSE 3000
CMD ["java", "-jar", "/srv/app.jar"]
