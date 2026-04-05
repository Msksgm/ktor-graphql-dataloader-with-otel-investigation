# ktor-graphql-dataloader-with-otel-investigation

## Abstract

This repository is a sample code of the below article.
If you want to know what is written in this repository, please read it.

## How to use.

### docker compose run

```bash
docker compose up --build
```

GraphQL endpoint is exposed on `http://localhost:8080/graphql`.
You can see Jaeger on `http://localhost:16686`.

You can also call graphql endpoint with curl like below commands.

```bash
curl -X POST http://localhost:8080/graphql \
    -H "Content-Type: application/json" \
    -d '{"query": "{ users { id name age booksWithNPlusOne { id title } } }"}'
```

```bash
curl -X POST http://localhost:8080/graphql \
    -H "Content-Type: application/json" \
    -d '{"query": "{ users { id name age booksWithDataLoader { id title } } }"}'
```

## shutdown with remove volumes and images

```bash
docker compose down -v --remove-orphans
```

## One liner to re-run

```bash
docker compose down -v --remove-orphans && docker compose up --build
```
