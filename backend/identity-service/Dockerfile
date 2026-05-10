# Multi-stage Docker build. Use bootBuildImage in CI for production images;
# this Dockerfile is the fallback for local image builds and is what kustomize
# references in dev overlays.
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /work
COPY ../../gradle ./gradle
COPY ../../gradlew ./gradlew
COPY ../../settings.gradle.kts ./settings.gradle.kts
COPY ../../build.gradle.kts ./build.gradle.kts
COPY ../../gradle.properties ./gradle.properties
COPY ../../platform-lib ./platform-lib
COPY . ./identity-service
RUN ./gradlew --no-daemon :identity-service:bootJar -x test

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN useradd -r -u 1001 -g root -s /usr/sbin/nologin tms
COPY --from=build /work/identity-service/build/libs/*.jar /app/app.jar
USER 1001
EXPOSE 8001
ENTRYPOINT ["java","-XX:+UseG1GC","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
