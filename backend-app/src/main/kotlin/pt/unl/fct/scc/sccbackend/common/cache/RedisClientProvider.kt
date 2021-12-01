package pt.unl.fct.scc.sccbackend.common.cache

import io.lettuce.core.RedisClient
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.coroutines.*
import org.springframework.stereotype.Service

private const val ENV_REDIS_CONNECTION_STRING = "REDIS_CONNECTION_STRING"

@Service
class RedisClientProvider {

    private val redis: RedisCoroutinesCommands<String, String>?

    init {
        val connectionString = System.getenv(ENV_REDIS_CONNECTION_STRING)
        if (connectionString != null && connectionString.isNotEmpty()) {
            this.redis = RedisClient.create(connectionString)
                .connect()
                .coroutines()
        } else {
            this.redis = null
        }
    }

    suspend fun <T> fetch(commands: suspend RedisCoroutinesCommands<String, String>.() -> T?): T? =
        redis?.let { commands(it) }

    suspend fun <T> run(commands: suspend RedisCoroutinesCommands<String, String>.() -> T?): Job =
        redis?.let { rd ->
            coroutineScope {
                launch { commands(rd) }
            }
        } ?: CompletableDeferred(value = null)

}