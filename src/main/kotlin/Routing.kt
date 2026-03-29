package com.example

import com.expediagroup.graphql.server.ktor.GraphQL
import com.expediagroup.graphql.server.ktor.defaultGraphQLStatusPages
import com.expediagroup.graphql.server.ktor.graphQLPostRoute
import com.expediagroup.graphql.server.operations.Query
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.*
import org.koin.ksp.generated.module
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin

fun Application.graphQLModule() {
    install(Koin) {
        modules(AppModule().module)
    }
    install(GraphQL) {
        schema {
            packages = listOf("com.example")
            queries = listOf(get<Query>())
        }
    }
    routing {
        graphQLPostRoute()
    }
    install(StatusPages) {
        defaultGraphQLStatusPages()
    }
}
