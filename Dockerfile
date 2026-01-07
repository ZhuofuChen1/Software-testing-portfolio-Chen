FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Copy Maven wrapper and pom.xml first for better layer caching
COPY mvnw ./
COPY .mvn ./.mvn
COPY pom.xml ./

# Download dependencies (cached if pom.xml unchanged)
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create storage directory for maintenance logs
RUN mkdir -p storage

EXPOSE 8080

# Set default ILP endpoint (can be overridden)
ENV ILP_ENDPOINT="https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net/"

# Run with optimized JVM settings
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
