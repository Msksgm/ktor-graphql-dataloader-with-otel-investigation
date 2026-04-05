FROM eclipse-temurin:21.0.10_7-jdk@sha256:3b0a98dfbdf1067c20a7854cec159551777d2ee1381bc76cd4bd0719f543b148 AS build

WORKDIR /workspace
COPY . .

RUN ./gradlew buildFatJar --no-daemon

FROM eclipse-temurin:21.0.10_7-jre@sha256:9d4453b48613404e1fa6e5e7eabc687ea57edfb8a1ad413956c5168f84b66af9

COPY --from=build /workspace/build/libs/ktor-graphql-dataloader-with-otel-investigation-all.jar /app.jar
COPY --from=build /workspace/build/container/opentelemetry-javaagent.jar /opentelemetry-javaagent.jar

ENTRYPOINT java -javaagent:/opentelemetry-javaagent.jar -jar /app.jar
