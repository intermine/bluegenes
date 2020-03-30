FROM gcr.io/distroless/java:11
COPY target/bluegenes.jar /
WORKDIR /
EXPOSE 5000
CMD ["bluegenes.jar"]
