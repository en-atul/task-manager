# Build argument for Java version
ARG JAVA_VERSION=21

# Stage 1: Build the application
FROM maven:3.9.4 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package

# Stage 2: Run the application
FROM openjdk:${JAVA_VERSION}-jdk-slim
WORKDIR /app
COPY --from=builder /app/target/task-manager.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]