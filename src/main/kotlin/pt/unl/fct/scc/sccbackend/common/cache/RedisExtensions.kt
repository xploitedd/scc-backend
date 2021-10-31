package pt.unl.fct.scc.sccbackend.common.cache

import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

suspend inline fun <reified T> RedisCoroutinesCommands<String, String>.setV(key: String, value: T): String? {
    return set(key, Json.encodeToString(value))
}

suspend inline fun <reified T> RedisCoroutinesCommands<String, String>.getV(key: String): T? {
    val value = get(key)
        ?: return null

    return Json.decodeFromString(value)
}

suspend inline fun <reified T> RedisCoroutinesCommands<String, String>.listPushHead(key: String, vararg values: T): Long? {
    val stringValues = values.map { Json.encodeToString(it) }
    return lpush(key, *stringValues.toTypedArray())
}

suspend inline fun <reified T> RedisCoroutinesCommands<String, String>.listPushTail(key: String, vararg values: T): Long? {
    val stringValues = values.map { Json.encodeToString(it) }
    return rpush(key, *stringValues.toTypedArray())
}

suspend inline fun <reified T> RedisCoroutinesCommands<String, String>.listPopHead(key: String, amount: Long = 1): List<T> {
    return lpop(key, amount)
        .map { Json.decodeFromString(it) }
}

suspend inline fun <reified T> RedisCoroutinesCommands<String, String>.listPopTail(key: String, amount: Long = 1): List<T> {
    return rpop(key, amount)
        .map { Json.decodeFromString(it) }
}

suspend inline fun <reified T> RedisCoroutinesCommands<String, String>.listRange(key: String, start: Long, end: Long): List<T> {
    return lrange(key, start, end)
        .map { Json.decodeFromString(it) }
}

suspend inline fun <reified T> RedisCoroutinesCommands<String, String>.listRemove(key: String, value: T, amount: Long = 1): Long? {
    return lrem(key, amount, Json.encodeToString(value))
}

suspend inline fun <reified T> RedisCoroutinesCommands<String, String>.listIndexOf(key: String, value: T): Long? {
    return lpos(key, Json.encodeToString(value))
}

suspend inline fun <reified T> RedisCoroutinesCommands<String, String>.setAdd(key: String, vararg values: T): Long? {
    val stringValues = values.map { Json.encodeToString(it) }
    return sadd(key, *stringValues.toTypedArray())
}

suspend inline fun <reified T> RedisCoroutinesCommands<String, String>.setRemove(key: String, vararg values: T): Long? {
    val stringValues = values.map { Json.encodeToString(it) }
    return srem(key, *stringValues.toTypedArray())
}

suspend inline fun <reified T> RedisCoroutinesCommands<String, String>.setExists(key: String, value: T): Boolean? {
    return sismember(key, Json.encodeToString(value))
}

suspend inline fun <reified T> RedisCoroutinesCommands<String, String>.setMembers(key: String): Set<T> {
    return smembers(key)
        .map { Json.decodeFromString<T>(it) }
        .toSet()
}