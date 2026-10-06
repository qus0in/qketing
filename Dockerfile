# syntax=docker/dockerfile:1
# ARM64 우선 multi-stage: 호스트/buildx arch를 그대로 사용 (FROM platform/arch 하드코딩 금지)

# ---------- build stage ----------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle ./
COPY gradle ./gradle
RUN chmod +x ./gradlew && ./gradlew --no-daemon --version

COPY . .
RUN ./gradlew --no-daemon bootJar -x test \
    && cp "$(ls build/libs/*.jar | head -n1)" /workspace/app.jar

# ---------- runtime stage ----------
FROM azul/zulu-openjdk-alpine:17-jre-headless AS runtime

RUN addgroup -S app && adduser -S -G app -u 10001 app \
    && mkdir -p /app && chown app:app /app

COPY --from=build --chown=app:app /workspace/app.jar /app/app.jar

USER app
WORKDIR /app

# secret/설정값은 image에 넣지 않음 (SPRING_PROFILES_ACTIVE 등은 런타임 env 주입)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
