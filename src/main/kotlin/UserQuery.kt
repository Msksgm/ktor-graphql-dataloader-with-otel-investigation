package com.example

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.generator.annotations.GraphQLIgnore
import com.expediagroup.graphql.server.exception.MissingDataLoaderException
import com.expediagroup.graphql.server.operations.Query
import graphql.GraphQLContext
import graphql.schema.DataFetchingEnvironment
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.context.Scope
import kotlinx.coroutines.runBlocking
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory
import org.jetbrains.exposed.sql.Except
import org.koin.core.annotation.Single
import java.util.concurrent.CompletableFuture

private val tracer = GlobalOpenTelemetry.getTracer("com.example.app")

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
        val span = tracer.spanBuilder("booksWithNPlusOne").startSpan()
        val scope: Scope = span.makeCurrent()
        try {
            span.setAttribute("app.component", "book-service-with-n+1-problem")
            return bookService.readByUserId(id).map { book ->
                BookResolver(id = book.id, title = book.name)
            }
        } catch (e: Exception) {
            span.recordException(e)
            throw e
        } finally {
            scope.close()
            span.end()
        }
    }

    @GraphQLDescription("user books via DataLoader")
    fun booksWithDataLoader(dataFetchingEnvironment: DataFetchingEnvironment): CompletableFuture<List<BookResolver>> {
        val span = tracer.spanBuilder("booksWithDataLoader").startSpan()
        span.setAttribute("userId", "$id")
        val scope: Scope = span.makeCurrent()
        return try {
            val loader = dataFetchingEnvironment
                    .getDataLoader<Int, List<BookResolver>>(BookDataLoader::class.simpleName!!)
                    ?: throw MissingDataLoaderException(BookDataLoader::class.simpleName!!)
            loader.load(id)
            } catch (e: Exception) {
                span.recordException(e)
                throw e
            } finally {
                scope.close()
                span.end()
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


@Single([KotlinDataLoader::class])
class BookDataLoader(
    private val bookService: BookService,
) : KotlinDataLoader<Int, List<BookResolver>> {
    override val dataLoaderName = BookDataLoader::class.simpleName!!

    override fun getDataLoader(graphQLContext: GraphQLContext): DataLoader<Int, List<BookResolver>> {
        return DataLoaderFactory.newDataLoader { userIds ->
            CompletableFuture.supplyAsync {
                val span = tracer.spanBuilder("CompletableFuture.supplyAsync").startSpan()
                val scope: Scope = span.makeCurrent()
                try {
                    runBlocking {
                        val booksByUserId = bookService.readBooksByUserIds(userIds).groupBy { it.userId }
                        userIds.map { userId ->
                            booksByUserId[userId]?.map { BookResolver(id = it.id, title = it.name) } ?: emptyList()
                        }
                    }
                } catch (e: Exception) {
                    span.recordException(e)
                    throw e
                } finally {
                    scope.close()
                    span.end()
                }
            }
        }
    }
}
