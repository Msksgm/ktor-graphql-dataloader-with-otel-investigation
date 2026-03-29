package com.example

import com.expediagroup.graphql.server.ktor.GraphQL
import com.expediagroup.graphql.server.ktor.defaultGraphQLStatusPages
import com.expediagroup.graphql.server.ktor.graphQLPostRoute
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import org.koin.dsl.module
import org.koin.ksp.generated.module
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin

fun Application.graphQLModule() {
    install(Koin) {
        modules(
            AppModule().module,
            module {
                single {
                    Database.connect(
                        url = "jdbc:postgresql://localhost:5432/mydatabase",
                        user = "postgres",
                        driver = "org.postgresql.Driver",
                        password = "password",
                    )
                }
            }
        )
    }
    install(GraphQL) {
        schema {
            packages = listOf("com.example")
            queries = listOf(get<HelloWorldQuery>(), get<UserQuery>())
        }
    }
    routing {
        graphQLPostRoute()
    }
    install(StatusPages) {
        defaultGraphQLStatusPages()
    }
}
