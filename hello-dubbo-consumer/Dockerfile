
FROM openjdk:15-alpine
RUN mkdir /app
COPY target/hello-dubbo-consumer-fat.jar  /app
WORKDIR /app
EXPOSE 20880
# ENTRYPOINT ["java","-jar","hello-dubbo-provider-fat.jar"]