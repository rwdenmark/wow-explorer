# Single-service image: builds the React SPA, bakes it into the Spring Boot jar's
# static resources, and runs the jar. Spring then serves the UI at / and the API at
# /api/** from the same origin (no CORS, no separate frontend host).
#
# Build context MUST be the repo root (this Dockerfile needs both frontend/ and backend/).

# ---- Stage 1: build the React frontend -> frontend/dist ----
FROM node:26-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ---- Stage 2: build the Spring Boot jar with the SPA baked in ----
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app
# Resolve dependencies first so this layer is cached unless pom.xml changes.
COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
# Spring Boot serves classpath:/static/** at the web root, so drop the built SPA there.
COPY --from=frontend /app/frontend/dist ./src/main/resources/static
RUN mvn -B -q clean package -DskipTests

# ---- Stage 3: runtime ----
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=backend /app/target/*.jar app.jar
# Use up to 75% of the container's memory for the heap (tuned for a 512 MB free tier).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
# No Docker daemon / compose file in the container: keep the local-dev auto-start off
# so startup doesn't try (and fail) to bring up a Postgres container.
ENV SPRING_DOCKER_COMPOSE_ENABLED=false
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
