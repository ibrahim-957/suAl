# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 1. Copy the wrapper files
COPY gradlew .
COPY gradle ./gradle

# 2. FIX: Grant execution permissions immediately
RUN chmod +x gradlew

# 3. Copy build configuration
COPY build.gradle settings.gradle ./

# 4. Download dependencies
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