FROM ubuntu:24.04

WORKDIR /app

RUN apt-get update && apt-get upgrade -y

RUN apt-get install openjdk-21-jdk maven -y

COPY . .

RUN mvn package

CMD cp ./target/*.jar dda-proxy.jar && \
    cp src/main/resources/application-docker.yaml application.yaml && \
    cp --update=none src/main/resources/config/categories.json config && \
    java -jar dda-proxy.jar