import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.ksp)
}

group = "com.example"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

val javaagent: Configuration by configurations.creating {
    description = "Dockerfileに埋め込むjavaagent"
}
dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgresql)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.graphql.kotlin.ktor.server)
    implementation(libs.koin.annotation)
    ksp(libs.koin.ksp.compiler)
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    javaagent(libs.opentelemetry.javaagent)
}

val destDir: Provider<Directory> = layout.buildDirectory.dir("container")
val downloadJavaagent by tasks.registering(Copy::class) {
    description = "Dockerfileに埋め込むjavaagentをダウンロードする"

    doFirst {
        mkdir(destDir)
    }
    from(javaagent.singleFile)
    into(destDir)
    // ダウンロードしたときに、"opentelemetry-javaagent-x.y.z.jar" が保存される
    // バージョンアップ時の影響箇所を最小にするために、"opentelemetry-javaagent.jar"に rename する
    rename {
        "opentelemetry-javaagent.jar"
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xannotation-default-target=param-property"))
}

tasks.named("buildFatJar") {
    dependsOn(downloadJavaagent)
}
