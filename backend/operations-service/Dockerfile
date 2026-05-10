FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /work
COPY ../../gradle ./gradle
COPY ../../gradlew ./gradlew
COPY ../../settings.gradle.kts ./settings.gradle.kts
COPY ../../build.gradle.kts ./build.gradle.kts
COPY ../../gradle.properties ./gradle.properties
COPY ../../platform-lib ./platform-lib
COPY . ./operations-service
RUN ./gradlew --no-daemon :operations-service:bootJar -x test

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN useradd -r -u 1001 -g root -s /usr/sbin/nologin tms
COPY --from=build /work/operations-service/build/libs/*.jar /app/app.jar
USER 1001
EXPOSE 8002
ENTRYPOINT ["java","-XX:+UseG1GC","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
