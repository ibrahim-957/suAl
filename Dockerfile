# ---- BUILD STAGE ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Wrapper ve build dosyalarını kopyala
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .
COPY src ./src

# Gradle izinleri
RUN chmod +x gradlew

# JAR oluştur
RUN ./gradlew clean build -x test

# ---- RUNTIME STAGE ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Build edilen JAR'ı kopyala
COPY --from=build /app/build/libs/SuAl-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
