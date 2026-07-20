FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the pre-built jar from the host target folder
COPY target/license-plate-game-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
