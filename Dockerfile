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
# Reset WORKDIR to / (matching the original distroless-based Dockerfile) so relative
# paths BlueGenes resolves at runtime (e.g. "./tools") match compose.yaml's mount
# target of /tools, instead of resolving under /bluegenes and never touching it.
WORKDIR /
CMD ["java", "-jar", "/bluegenes/target/bluegenes.jar"]
