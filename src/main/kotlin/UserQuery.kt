package com.example

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.server.operations.Query
import org.koin.core.annotation.Single

@Single([Query::class])
class UserQuery(
    private val userService: UserService,
    private val bookService: BookService,
) : Query {
    suspend fun user(id: Int): UserResolver? {
        val user = userService.read(id) ?: return null
        return UserResolver(
                id = id,
                name = user.name,
                age = user.age,
                bookService = bookService,
        )
    }

    suspend fun users(): List<UserResolver> {
        return userService.readAll().map { user ->
            UserResolver(
                id = user.id,
                name = user.name,
                age = user.age,
                bookService = bookService,
            )
        }
    }
}

@GraphQLDescription("user")
data class UserResolver(
    @GraphQLDescription("user id")
    val id: Int,

    @GraphQLDescription("user name")
    val name: String,

    @GraphQLDescription("user age")
    val age: Int,

    @GraphQLIgnore
    val bookService: BookService,
) {
    @GraphQLDescription("user books")
    suspend fun booksWithNPlusOne(): List<BookResolver> {
        return bookService.readByUserId(id).map { book ->
            BookResolver(id = book.id, title = book.name)
        }
    }
}

@GraphQLDescription("book")
data class BookResolver(
    @GraphQLDescription("book id")
    val id: Int,

    @GraphQLDescription("book title")
    val title: String,
)
