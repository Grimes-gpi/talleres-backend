# Imagen del backend para Render (o cualquier servicio que use Docker).
# Etapa 1: compila el .jar con el mismo Gradle wrapper del proyecto.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

# Etapa 2: solo lo necesario para ejecutar.
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
# Render pasa el puerto en la variable PORT; application.properties la lee.
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
