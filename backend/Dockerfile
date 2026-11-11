# syntax=docker/dockerfile:1
# ---------- Construction ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml spotbugs-exclude.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package \
    && cp target/backend-*.jar /app/app.jar

# ---------- Exécution ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Utilisateur sans privilèges (sécurité)
RUN addgroup -S campus && adduser -S campus -G campus
COPY --from=build /app/app.jar app.jar
USER campus
ENV SERVER_PORT=8280 \
    SHOW_SQL=false \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8280
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=5 \
    CMD wget -qO- "http://127.0.0.1:${SERVER_PORT}/api/health" >/dev/null || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
