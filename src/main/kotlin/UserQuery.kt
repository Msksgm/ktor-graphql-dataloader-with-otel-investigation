package com.example

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import org.koin.core.annotation.Single

@Single([Query::class])
class UserQuery(private val userService: UserService) : Query {
    suspend fun user(id: Int): UserResolver? {
        val user = userService.read(id) ?: return null
        return UserResolver(
                id = id,
                name = user.name,
                age = user.age,
        )
    }
}

@GraphQLDescription("user")
data class UserResolver (
    @GraphQLDescription("user id")
    val id: Int,

    @GraphQLDescription("user name")
    val name: String,

    @GraphQLDescription("user age")
    val age: Int,
)
