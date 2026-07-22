# ---- Build stage: compiles the jar inside the image, so `docker compose up --build`
# is self-contained and doesn't require a pre-built target/ folder on the host ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copy just the wrapper/pom first so dependency downloads are cached across
# rebuilds that only change application source code.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src ./src
RUN ./mvnw -B package -DskipTests

# ---- Run stage: only the JRE and the built jar, no build tooling ----
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/license-plate-game-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
