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
| `./gradlew run` | サーバー起動（localhost:8080） |
| `./gradlew buildFatJar` | 全依存を含む実行可能 JAR をビルド（OpenTelemetry javaagent も自動ダウンロード） |
| `./gradlew buildImage` | Docker イメージをビルド |
| `docker compose up` | PostgreSQL + OpenTelemetry Collector + アプリを一括起動 |

ローカルで `./gradlew run` する場合は、事前に PostgreSQL が localhost:5432 で起動している必要がある（`docker compose up db` で DB のみ起動可能）。
アプリは `DB_HOST` 環境変数を見て接続先を切り替える。未指定時は `localhost`、Docker Compose では `db` が使われる。

## アーキテクチャ

### 技術スタック
- **Kotlin 2.3 / JVM 21** / Ktor 3.4（Netty）
- **GraphQL Kotlin 9.1**（Expedia Group）— スキーマ自動生成（code-first）
- **Exposed 0.61** — Kotlin ORM（PostgreSQL 17）
- **Koin 4.2 + KSP** — DI（`@Single` アノテーションでコンパイル時生成）
- **OpenTelemetry JavaAgent** — トレース・メトリクス・ログ計装

### ソース構成（`src/main/kotlin`）

ソースファイル自体は `src/main/kotlin` 直下にあり、すべて `com.example` パッケージに属している。

- `Application.kt` — エントリーポイント（`EngineMain.main`）
- `Routing.kt` — Koin・GraphQL・DB 接続の初期化（`graphQLModule()`）
- `AppModule.kt` — Koin モジュール定義（KSP の `@Module` / `@ComponentScan`）
- `UserQuery.kt` — GraphQL クエリ（`user`, `users`）、`UserResolver`、`BookResolver`、`BookDataLoader`
- `UsersSchema.kt` — `UserService`（Users テーブル CRUD）
- `BookService.kt` — `BookService`（Books テーブル CRUD）
- `HelloWorldQuery.kt` — 動作確認用の `helloWorld` クエリ

### GraphQL エンドポイント

`POST /graphql` のみ。Ktor の `graphQLPostRoute()` を使って公開しており、スキーマは `com.example` パッケージから code-first で自動生成される。

### DataLoader パターン

`UserResolver` に 2 つのフィールドリゾルバがある：
- `booksWithNPlusOne()` — ユーザーごとに個別クエリ（N+1 問題を再現）
- `booksWithDataLoader()` — `BookDataLoader` でバッチ取得（`readBooksByUserIds` で `IN` 句による一括クエリ）

GraphQL の query ルートは `HelloWorldQuery` と `UserQuery` の 2 つで構成される。DataLoader は Koin から `KotlinDataLoader` として収集され、`KotlinDataLoaderRegistryFactory` に登録される。

### DB 構成

- PostgreSQL 17（`mydatabase` / `postgres:password`）
- 接続先ホストは環境変数 `DB_HOST`（デフォルト `localhost`）
- テーブル: `Users`（id, name, age）、`Books`（id, name, user_id）
- `UserService` / `BookService` の初期化時に `SchemaUtils.create(...)` でテーブル作成を行う
- シードデータ: `initdb/01_seed.sql`（Docker Compose 起動時に自動投入）

### OpenTelemetry / Docker Compose

- `docker compose up` では `app`、`db`、`collector`、`jaeger` が起動する
- アプリは `OTEL_EXPORTER_OTLP_ENDPOINT=http://collector:4318` を使って Collector に送信する
- Jaeger UI は `http://localhost:16686` で確認できる

## 実装上の注意

- 依存関係注入は Koin Annotations ベース。手動で DI を増やす前に `@Single` や `@Module` を確認する
- DB アクセスは Exposed + `newSuspendedTransaction(Dispatchers.IO)` を使っているため、同じパターンに合わせる
- このリポジトリには現時点でテストコードがない。変更時は必要に応じてテスト追加も検討する
