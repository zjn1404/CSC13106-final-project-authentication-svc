# Multi-stage build for Spring Boot Authentication Service

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy pom.xml and download dependencies (with retry for network issues)
COPY pom.xml .
RUN for i in 1 2 3; do \
      mvn dependency:go-offline -B && break; \
      echo "Retry $i failed, waiting..."; \
      sleep 5; \
    done

# Copy source code and build (with retry for network issues)
COPY src ./src
RUN for i in 1 2 3; do \
      mvn clean package -DskipTests -B && break; \
      echo "Retry $i failed, waiting..."; \
      sleep 5; \
    done || (echo "Maven build failed after 3 retries" && exit 1)

# Stage 2: Runtime
# Using standard JRE instead of Alpine to avoid netty native library issues
FROM eclipse-temurin:21-jre
WORKDIR /app

# Create non-root user
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose port
EXPOSE 8081

# Health check using curl (available in base image)
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

