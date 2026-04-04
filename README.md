# ktor-graphql-dataloader-with-otel-investigation

## Abstract

This repository is a sample code of the below article.
If you want to know what is written in this repository, please read it.



## How to use.

### build

```bash
./gradlew clean buildFatJar
```

### docker compose run

```bash
docker compose up --build
```

You can see Jaeger on `http://localhost:16686`.

### One liner to re-run

```bash
./gradlew clean buildFatJar && docker compose down -v --remove-orphans && docker compose up --build
```
