FROM openjdk:17-jdk-slim

COPY target/app.jar .

ENTRYPOINT ["java", "-jar", "app.jar"]