# syntax=docker/dockerfile:1

# ==============================================================================
# Build Stage
# ==============================================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy application source code and build production artifact
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==============================================================================
# Production Runtime Stage
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Install curl for container healthcheck & tzdata for accurate timestamps
RUN apk add --no-cache curl tzdata && \
    addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Configure JVM & Spring Boot production defaults
ENV PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom"

# Copy built JAR from builder stage with proper ownership
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar app.jar

# Run as non-root user for security
USER appuser:appgroup

# Expose default application port
EXPOSE 8080

# Production Health Check
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:${PORT:-8080}/api/admin/auth/check || exit 1

# Launch application with exec to forward OS signals (SIGTERM/SIGINT) directly to JVM for graceful shutdown
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
