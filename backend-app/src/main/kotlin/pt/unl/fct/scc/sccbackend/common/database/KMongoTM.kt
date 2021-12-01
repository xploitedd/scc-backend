package pt.unl.fct.scc.sccbackend.common.database

import com.mongodb.client.model.Indexes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.commitTransactionAndAwait
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.id.serialization.IdKotlinXSerializationModule
import org.litote.kmongo.reactivestreams.KMongo
import org.litote.kmongo.serialization.registerModule
import org.springframework.stereotype.Component
import pt.unl.fct.scc.sccbackend.channels.model.Channel
import pt.unl.fct.scc.sccbackend.channels.model.ChannelMessage
import pt.unl.fct.scc.sccbackend.media.model.Media
import pt.unl.fct.scc.sccbackend.users.model.User
import pt.unl.fct.scc.sccbackend.users.model.UserChannel

private const val ENV_CONNECTION_STR = "DB_CONNECTION_STRING"
private const val ENV_DATABASE_NAME = "DB_NAME"

@Component
class KMongoTM {

    private val connectionString = System.getenv(ENV_CONNECTION_STR)
        ?: throw Exception("The $ENV_CONNECTION_STR environment variable is not defined")

    private val databaseName = System.getenv(ENV_DATABASE_NAME)
        ?: throw Exception("The $ENV_DATABASE_NAME environment variable is not defined")

    private val kMongo = KMongo.createClient(connectionString)
        .coroutine

    private val database = kMongo.getDatabase(databaseName)

    init {
        registerModule(IdKotlinXSerializationModule)

        // create indexes
        runBlocking {
            launch {
                use { db ->
                    val userCol = db.getCollection<User>()
                    userCol.createIndex(ascendingIndex(User::nickname))

                    val mediaCol = db.getCollection<Media>()
                    mediaCol.createIndex(ascendingIndex(Media::hash))

                    val channelCol = db.getCollection<Channel>()
                    channelCol.createIndex(ascendingIndex(Channel::name))
                    channelCol.createIndex(ascendingIndex(Channel::owner))

                    val channelMessagesCol = db.getCollection<ChannelMessage>()
                    channelMessagesCol.createIndex(ChannelMessage::text.textIndex())
                    channelMessagesCol.createIndex(ascendingIndex(ChannelMessage::channelId))
                    channelMessagesCol.createIndex(descendingIndex(ChannelMessage::createdAt))
                    channelMessagesCol.createIndex(ascendingIndex(
                        ChannelMessage::user,
                        ChannelMessage::replyTo
                    ))

                    val userChannelsCol = db.getCollection<UserChannel>()
                    userChannelsCol.createIndex(ascendingIndex(UserChannel::user))
                    userChannelsCol.createIndex(ascendingIndex(UserChannel::channel))
                }
            }
        }
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