FROM eclipse-temurin:17-jdk-slim

WORKDIR /app

COPY *.jar app.jar

ENTRYPOINT ["java", "-Dserver.address=0.0.0.0", "-Xms64m", "-Xmx128m", "-jar", "app.jar"]