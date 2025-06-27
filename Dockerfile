# syntax=docker/dockerfile:1
FROM node:lts-alpine AS bluegenes_build

WORKDIR /bluegenes
COPY . .

RUN apk update && \
    apk add openjdk11 leiningen git;
ENV JAVA_HOME="/usr/lib/jvm/java-11-openjdk-amd64/"

RUN npm install;

RUN lein uberjar;

EXPOSE 5000
CMD ["java", "-jar", "target/bluegenes.jar"]
