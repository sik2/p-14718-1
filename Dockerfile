FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

ARG MODULE

COPY gradlew .
COPY gradle gradle
COPY settings.gradle.kts .

RUN mkdir -p common ${MODULE}
COPY common/build.gradle.kts common/
COPY ${MODULE}/build.gradle.kts ${MODULE}/

RUN printf 'rootProject.name = "back"\ninclude("common")\ninclude("%s")\n' "${MODULE}" > settings.gradle.kts \
    && chmod +x gradlew

RUN ./gradlew dependencies --no-daemon || true

COPY common/src/ common/src/
COPY ${MODULE}/src/ ${MODULE}/src/

RUN ./gradlew :${MODULE}:bootJar --no-daemon -x test \
    && rm -rf ~/.gradle/caches ~/.gradle/daemon

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

ARG MODULE

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

COPY --from=build /app/${MODULE}/build/libs/*.jar app.jar

USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]
