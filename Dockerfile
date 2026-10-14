# ==============================================================================
# Stage 1: Build Application with Maven & OpenJDK 21 on Red Hat UBI 9
# ==============================================================================
FROM registry.access.redhat.com/ubi9/openjdk-21:latest AS builder

USER 0

WORKDIR /build

# Copy Maven descriptor and download dependencies for layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy application source code
COPY src ./src

# Build production executable JAR (skipping unit/integration tests during image build)
RUN mvn clean package -DskipTests -B

# Extract Spring Boot layered JAR for optimal container caching
WORKDIR /build/target
RUN java -Djarmode=layertools -jar book-worm-backend-1.0.0.jar extract

# ==============================================================================
# Stage 2: Minimal Production Runtime on Red Hat UBI 9 OpenJDK 21 Runtime
# ==============================================================================
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:latest

LABEL maintainer="Book Worm Engineering Team" \
      description="Book Worm E-Commerce Backend Service" \
      version="1.0.0"

USER 0

# Install curl for health checking if not present
RUN microdnf update -y && \
    microdnf install -y curl && \
    microdnf clean all

# Create dedicated non-root application user and group
RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -m -s /bin/bash appuser

WORKDIR /app

# Copy extracted layers from builder stage
COPY --from=builder --chown=1001:1001 /build/target/dependencies/ ./
COPY --from=builder --chown=1001:1001 /build/target/spring-boot-loader/ ./
COPY --from=builder --chown=1001:1001 /build/target/snapshot-dependencies/ ./
COPY --from=builder --chown=1001:1001 /build/target/application/ ./

# Switch to non-root user
USER 1001

# Expose internal application port
EXPOSE 8080

# Configure JVM tuning and production flags
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom"

# Container Healthcheck using Spring Boot Actuator probe
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://127.0.0.1:8080/actuator/health/liveness || exit 1

# Launch application via Spring Boot JarLauncher
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
