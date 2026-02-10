# Multi-stage build for Spring Boot Authentication Service

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Configure Maven to use Central repository
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?><settings><mirrors><mirror><id>central</id><name>Maven Central</name><url>https://repo.maven.apache.org/maven2</url><mirrorOf>*</mirrorOf></mirror></mirrors></settings>' > /root/.m2/settings.xml

# Copy pom.xml and download dependencies (with retry)
COPY pom.xml .
RUN for i in 1 2 3 4 5; do \
      mvn dependency:go-offline -B && break || \
      (echo "Maven dependency attempt $i failed, retrying in 15s..." && sleep 15); \
    done

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B && \
    ls -lh /app/target/*.jar | grep -v sources | grep -v javadoc

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

