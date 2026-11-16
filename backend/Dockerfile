# syntax=docker/dockerfile:1.7
# Production image: multi-stage, non-root, UTC, no build/runtime secret.
# Build targets:
#   docker build --target runtime -t lifeos-backend .
#   docker build --target migrations -t lifeos-migrations .

FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -q package -DskipTests

# Pre-deploy image, chạy bằng DB migration role; không dùng credential runtime app.
FROM flyway/flyway:12.4.0-alpine AS migrations
COPY src/main/resources/db/migration /flyway/sql
COPY docker/migration-entrypoint.sh /usr/local/bin/lifeos-migration
RUN chmod 0555 /usr/local/bin/lifeos-migration
ENTRYPOINT ["/usr/local/bin/lifeos-migration"]
CMD ["idle"]

FROM eclipse-temurin:21-jre-jammy AS runtime
ARG APP_UID=10001
ARG APP_GID=10001
RUN groupadd --system --gid "${APP_GID}" lifeos \
    && useradd --system --uid "${APP_UID}" --gid lifeos --no-create-home --shell /usr/sbin/nologin lifeos \
    && install -d -o lifeos -g lifeos /app /var/lib/lifeos/exports
WORKDIR /app
COPY --from=build --chown=lifeos:lifeos /src/target/lifeos-backend-*.jar /app/app.jar

ENV TZ=UTC \
    JAVA_TOOL_OPTIONS="-Duser.timezone=UTC -XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError" \
    EXPORT_DIR=/var/lib/lifeos/exports

USER ${APP_UID}:${APP_GID}
EXPOSE 8080
STOPSIGNAL SIGTERM
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=4 \
  CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /actuator/health/liveness HTTP/1.0\\r\\nHost: localhost\\r\\n\\r\\n' >&3 && grep -q '\"status\":\"UP\"' <&3"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
