package com.example

import com.expediagroup.graphql.server.ktor.GraphQL
import com.expediagroup.graphql.server.ktor.defaultGraphQLStatusPages
import com.expediagroup.graphql.server.ktor.graphQLPostRoute
import com.expediagroup.graphql.server.operations.Query
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
    }
}

class HelloWorldQuery: Query {
    fun hello(): String = "Hello World!"
}

fun Application.graphQLModule() {
    install(GraphQL) {
        schema {
            packages = listOf("com.example")
            queries = listOf(
                HelloWorldQuery()
            )
        }
    }
    routing {
        graphQLPostRoute()
    }
    install(StatusPages) {
        defaultGraphQLStatusPages()
    }
}
