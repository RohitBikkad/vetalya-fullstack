# Multi-stage Docker build for Vetalya Spring Boot Backend
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Ensure maven wrapper is executable and download dependencies
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B || true

# Copy source code and build production jar
COPY src src
RUN ./mvnw clean package -DskipTests -B

# Runtime Image: Ubuntu Jammy with full Glibc, updated CA certificates, and curl
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install ca-certificates and curl
RUN apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates curl && \
    update-ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Create non-root application user and ensure log directory exists
RUN useradd -m -s /bin/bash vetalya && \
    mkdir -p /var/log/vetalya /app && \
    chown -R vetalya:vetalya /var/log/vetalya /app

# Copy compiled jar from builder
COPY --from=builder /workspace/target/Vetalya-0.0.1-SNAPSHOT.jar app.jar

USER vetalya
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod
ENV MANAGEMENT_PORT=8080

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.net.preferIPv4Stack=true", \
    "-Djava.net.preferIPv4Addresses=true", \
    "-jar", "app.jar"]

