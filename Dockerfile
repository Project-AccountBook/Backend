FROM openjdk:17-jdk-slim

COPY build/libs/AccountBookForMoms-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-Dserver.address=0.0.0.0", "-Xms64m", "-Xmx128m", "-jar", "app.jar"]