# The push server's image. Build from the repository root (the server shares the app's version catalog and wrapper):
#   docker build -f server/Dockerfile -t maro-push .
# The service account key is never part of the image: pass it at run time (see server/README.md).

# --- Build ---
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src

# Wrapper and build scripts first, so the dependency layer is reused while only the sources change.
COPY gradlew ./
COPY gradle ./gradle
COPY server/settings.gradle.kts server/build.gradle.kts ./server/
# A Windows checkout gives gradlew CRLF line endings, which Linux cannot run.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew --no-daemon -p server dependencies > /dev/null

COPY server/src ./server/src
RUN ./gradlew --no-daemon -p server installDist

# --- Run ---
FROM eclipse-temurin:17-jre
RUN useradd --system --no-create-home maro
WORKDIR /app
COPY --from=build /src/server/build/install/maro-server ./

# Small hostings give 256-512 MB: let the JVM size its heap by the container's limit.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENV PORT=8080
EXPOSE 8080

# The hosting checks GET /health -> "ok" itself (no Docker HEALTHCHECK: the JRE image has no curl).
USER maro
ENTRYPOINT ["./bin/maro-server"]
