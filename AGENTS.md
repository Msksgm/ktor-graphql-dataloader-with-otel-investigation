# AGENTS.md

This file provides guidance for coding agents working with code in this repository.

## プロジェクト概要

Ktor + GraphQL Kotlin + DataLoader を使った、GraphQL における N+1 問題の解決パターンを示すサンプルプロジェクト。
OpenTelemetry JavaAgent による計装も含む。

## 主要コマンド

| コマンド | 説明 |
|---|---|
| `./gradlew build` | ビルド |
| `./gradlew test` | テスト実行 |
| `./gradlew run` | サーバー起動（`localhost:8080`） |
| `./gradlew buildFatJar` | 全依存を含む実行可能 JAR をビルドし、JavaAgent を `build/container/opentelemetry-javaagent.jar` に配置 |
| `./gradlew buildImage` | Docker イメージをビルド |
| `docker compose up --build` | アプリ、PostgreSQL、OpenTelemetry Collector、Jaeger を一括起動 |
| `docker compose up db` | ローカル実行用に PostgreSQL のみ起動 |

ローカルで `./gradlew run` する場合は、事前に PostgreSQL が `localhost:5432` で起動している必要がある。
アプリは `DB_HOST` 環境変数で接続先を切り替える。未指定時は `localhost`、Docker Compose では `db` が使われる。

## アーキテクチャ

### 技術スタック
- **Kotlin 2.3.0 / JVM 21**
- **Ktor 3.4.1**（Netty）
- **GraphQL Kotlin 9.1.0**（code-first）
- **Exposed 0.61.0** + **PostgreSQL JDBC 42.7.10**
- **Koin 4.2.0 + Koin Annotations/KSP**
- **OpenTelemetry JavaAgent 2.26.1** + **OpenTelemetry API 1.60.1**

### ソース構成（`src/main/kotlin`）

ソースファイルは `src/main/kotlin` 直下にあり、すべて `com.example` パッケージに属している。

- `Application.kt` — エントリーポイント。`module()` から `graphQLModule()` を呼ぶ
- `Routing.kt` — Koin、DB 接続、GraphQL、DataLoader Registry、`POST /graphql` ルート、StatusPages の初期化
- `AppModule.kt` — Koin Annotations 用の `@Module` / `@ComponentScan`
- `HelloWorldQuery.kt` — 動作確認用の `helloWorld` クエリ
- `UserQuery.kt` — `user`, `users` クエリ、`UserResolver`、`BookResolver`、`BookDataLoader`
- `UsersSchema.kt` — `Users` テーブル定義と `UserService`
- `BookService.kt` — `Books` テーブル定義と `BookService`

### GraphQL エンドポイント

`POST /graphql` のみを公開している。
Ktor の `graphQLPostRoute()` を使っており、スキーマは `com.example` パッケージから code-first で生成される。
Query ルートは `HelloWorldQuery` と `UserQuery` の 2 つ。

### DI と初期化

- Koin は `Routing.kt` でインストールされる
- `AppModule().module` から KSP 生成済みの定義を読み込む
- `Database.connect(...)` は Koin の `single` として登録される
- DataLoader は `getAll<KotlinDataLoader<*, *>>()` で集約され、`KotlinDataLoaderRegistryFactory` に渡される

### DataLoader パターン

`UserResolver` に 2 つのフィールドリゾルバがある。

- `booksWithNPlusOne()` — ユーザーごとに `BookService.readByUserId` を呼び、N+1 問題を再現する
- `booksWithDataLoader()` — `BookDataLoader` を使ってバッチ取得する

`BookDataLoader` は `readBooksByUserIds(userIds)` を使って `IN` 句で一括取得し、`userId` ごとに結果を再構成して返す。

### DB 構成

- Docker Compose の DB イメージは **PostgreSQL 18.3**
- DB 名は `mydatabase`
- 認証情報は `postgres` / `password`
- 接続先ホストは環境変数 `DB_HOST`（デフォルト `localhost`）
- テーブルは `Users`（id, name, age）と `Books`（id, name, user_id）
- `UserService` / `BookService` の初期化時に `SchemaUtils.create(...)` でテーブル作成を行う
- シードデータは `initdb/01_seed.sql` から投入される

### OpenTelemetry / Docker Compose

- `docker compose up --build` では `app`、`db`、`collector`、`jaeger` が起動する
- アプリは `-javaagent:/workspace/build/container/opentelemetry-javaagent.jar` で JavaAgent を有効化する
- OTLP エンドポイントは `http://collector:4318`
- Compose では `OTEL_LOGS_EXPORTER=none`、`OTEL_METRICS_EXPORTER=none` が設定されている
- Jaeger UI は `http://localhost:16686`

### アプリ設定

- Ktor のポートは `src/main/resources/application.yaml` で `8080`
- GraphQL Playground や追加ルーティングはなく、GraphQL POST のみを公開している

## 実装上の注意

- 依存関係注入は Koin Annotations ベース。新しいサービスや Query/DataLoader を追加する場合は、まず `@Single` と `@ComponentScan` で拾えるか確認する
- DB アクセスは Exposed + `newSuspendedTransaction(Dispatchers.IO)` パターンに揃える
- `UserResolver` と `BookDataLoader` には OpenTelemetry span の手動計装が入っている。関連処理を変更する場合は span の開始・終了と例外記録を崩さない
- `BookDataLoader` は `BookDataLoader::class.simpleName!!` を DataLoader 名として使っている。参照側と登録側の名前をずらさない
- 現時点で `src/test` は存在しない。変更が振る舞いに影響する場合はテスト追加を検討する
