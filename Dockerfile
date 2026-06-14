FROM ubuntu:24.04

WORKDIR /app

RUN apt-get update && apt-get upgrade -y

RUN apt-get install openjdk-21-jdk maven -y

COPY . .

RUN mvn package

COPY target/*.jar dda-proxy.jar

COPY src/main/resources/application-docker.yaml application.yaml

CMD java -jar dda-proxy.jar