FROM gradle:8-jdk21 AS build
WORKDIR /app
COPY . .
# Build the application
RUN ./gradlew installDist --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/install/heraldo-gestor /app/

# Expose Ktor API port and Google Auth receiver port
EXPOSE 8080 8888

CMD ["/app/bin/heraldo-gestor"]
