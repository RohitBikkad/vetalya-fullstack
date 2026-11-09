# Multi-stage Docker build for Vetalya Spring Boot Backend
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Install bash/curl for maven wrapper
RUN apk add --no-cache bash

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B || true

# Copy source code and build production jar
COPY src src
RUN ./mvnw clean package -DskipTests -B

# Runtime Image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S vetalya && adduser -S vetalya -G vetalya

# Copy compiled jar from builder
COPY --from=builder /workspace/target/Vetalya-0.0.1-SNAPSHOT.jar app.jar

USER vetalya
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod
ENV MANAGEMENT_PORT=8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
