package pt.unl.fct.scc.sccbackend.common.database

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.litote.kmongo.coroutine.CoroutineClient
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.commitTransactionAndAwait
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.id.serialization.IdKotlinXSerializationModule
import org.litote.kmongo.reactivestreams.KMongo
import org.litote.kmongo.serialization.registerModule
import org.springframework.stereotype.Component

private const val ENV_CONNECTION_STR = "DB_CONNECTION_STRING"
private const val ENV_DATABASE_NAME = "DB_NAME"

@Component
class KMongoTM {

    private val connectionString = System.getenv(ENV_CONNECTION_STR)
        ?: throw Exception("The $ENV_CONNECTION_STR environment variable is not defined")

    private val databaseName = System.getenv(ENV_DATABASE_NAME)
        ?: throw Exception("The $ENV_DATABASE_NAME environment variable is not defined")

    private val kMongo: CoroutineClient by lazy {
        KMongo.createClient(connectionString)
            .coroutine
    }

    private val database: CoroutineDatabase by lazy {
        kMongo.getDatabase(databaseName)
    }

    init {
        registerModule(IdKotlinXSerializationModule)
    }

    suspend fun <T> use(operation: suspend (CoroutineDatabase) -> T): T {
        return withContext(Dispatchers.IO) {
            operation(database)
        }
    }

    suspend fun <T> useTransaction(transaction: suspend (CoroutineDatabase) -> T): T {
        return withContext(Dispatchers.IO) {
            kMongo.startSession().use {
                it.startTransaction()
                val res = transaction(database)
                it.commitTransactionAndAwait()

                res
            }
        }
    }

}