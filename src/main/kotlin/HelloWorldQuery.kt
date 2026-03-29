package com.example

import com.expediagroup.graphql.server.operations.Query
import org.koin.core.annotation.Single

@Single([Query::class])
class HelloWorldQuery: Query {
    fun hello(): String = "Hello World!"
}
