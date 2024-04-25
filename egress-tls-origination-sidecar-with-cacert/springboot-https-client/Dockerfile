FROM openjdk:17-jdk-alpine
USER root
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]