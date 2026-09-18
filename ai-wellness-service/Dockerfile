# Stage 1: Build Java 21 Spring Boot executable JAR
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Package application (skip tests for build container speed)
RUN mvn clean package -DskipTests

# Stage 2: Lightweight Java 21 Runtime container
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy built JAR from build stage
COPY --from=build /app/target/ai-wellness-service-1.0.0-SNAPSHOT.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run Spring Boot app on port 8080
ENTRYPOINT ["java", "-Dserver.port=8080", "-jar", "app.jar"]
