package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.annotation.Single

@Serializable
data class ExposedBook(val id: Int, val name: String, val userId: Int)

@Single
class BookService(database: Database) {
    object Books : Table() {
        val id = integer("id").autoIncrement()
        val name = varchar("name", length = 100)
        val userId = integer("user_id")

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Books)
        }
    }

    suspend fun readByUserId(userId: Int): List<ExposedBook> {
        return dbQuery {
            Books.selectAll()
                .where { Books.userId eq userId }
                .map { ExposedBook(it[Books.id], it[Books.name], it[Books.userId]) }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
