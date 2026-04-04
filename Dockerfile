FROM eclipse-temurin:21.0.10_7-jre@sha256:9d4453b48613404e1fa6e5e7eabc687ea57edfb8a1ad413956c5168f84b66af9

COPY build/libs/ktor-graphql-dataloader-with-otel-investigation-all.jar /app.jar
COPY build/container/opentelemetry-javaagent.jar /opentelemetry-javaagent.jar

ENTRYPOINT java -javaagent:/opentelemetry-javaagent.jar -jar /app.jar
