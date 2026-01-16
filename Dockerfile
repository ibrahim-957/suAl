# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy gradle executable tools
COPY gradlew .
COPY gradle ./gradle

# Copy build configuration
COPY build.gradle settings.gradle ./

# NEW: Download dependencies (this layer is cached unless build.gradle changes)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src ./src

# Build the application
RUN ./gradlew clean build -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Use a wildcard to find the jar or ensure a consistent name
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]