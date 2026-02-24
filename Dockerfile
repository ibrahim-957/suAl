# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 1. Copy wrapper files
COPY gradlew .
COPY gradle ./gradle
RUN chmod +x gradlew

# 2. Copy build config and download dependencies
#    This layer is CACHED until build.gradle or settings.gradle changes
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon

# 3. Copy source and build
#    Only this layer re-runs when your code changes
COPY src ./src
RUN ./gradlew build -x test --no-daemon --build-cache

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]