# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S gym && adduser -S gym -G gym
WORKDIR /app
COPY --from=build /workspace/target/GymApp-*.jar app.jar
USER gym
EXPOSE 8080
# No credentials here: DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET are provided at container startup.
ENV SPRING_PROFILES_ACTIVE=dev
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
