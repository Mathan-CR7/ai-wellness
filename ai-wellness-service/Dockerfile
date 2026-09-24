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

COPY --from=build /app/target/ai-wellness-service-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]

