package pt.unl.fct.scc.sccbackend.channels.repo

import io.lettuce.core.ScoredValue
import kotlinx.coroutines.flow.collect
import org.litote.kmongo.and
import org.litote.kmongo.coroutine.updateOne
import org.litote.kmongo.eq
import org.springframework.stereotype.Repository
import pt.unl.fct.scc.sccbackend.channels.model.Channel
import pt.unl.fct.scc.sccbackend.channels.model.ChannelMessage
import pt.unl.fct.scc.sccbackend.common.BadRequestException
import pt.unl.fct.scc.sccbackend.common.NotFoundException
import pt.unl.fct.scc.sccbackend.common.cache.*
import pt.unl.fct.scc.sccbackend.common.database.KMongoTM
import pt.unl.fct.scc.sccbackend.common.pagination.Pagination
import pt.unl.fct.scc.sccbackend.media.model.Media
import pt.unl.fct.scc.sccbackend.users.model.User
import pt.unl.fct.scc.sccbackend.users.model.UserChannel

@Repository
class ChannelRepositoryImpl(
    val tm: KMongoTM,
    val redis: RedisClientProvider
) : ChannelRepository {

    override suspend fun getChannels(user: User?, pagination: Pagination): Set<Channel> {
        val cached = redis.use { setMembers<Channel>("channels") }
        if (!cached.isNullOrEmpty()) {
            return cached.filter { !it.private || (user != null && isUserInChannel(it, user)) }
                .drop(pagination.offset)
                .take(pagination.limit)
                .toSet()
        }

        return tm.use { db ->
            val col = db.getCollection<Channel>()
            val channels = col.find()
                .toList()

            if (channels.isNotEmpty())
                redis.use { setAdd("channels", *channels.toTypedArray()) }

            channels.filter { !it.private || (user != null && isUserInChannel(it, user)) }
                .drop(pagination.offset)
                .take(pagination.limit)
                .toSet()
        }
    }

    override suspend fun createChannel(channel: Channel) = tm.use { db ->
        val col = db.getCollection<Channel>()
        col.insertOne(channel)

        redis.use {
            setV("channel:${channel.channelId}", channel)
            setAdd("channels", channel)
        }

        channel
    }

    override suspend fun getChannel(channelId: String): Channel {
        val cached = redis.use { getV<Channel>("channel:${channelId}") }
        if (cached != null)
            return cached

        return tm.use { db ->
            val col = db.getCollection<Channel>()
            val channel = col.findOne(Channel::channelId eq channelId)
                ?: throw NotFoundException()

            redis.use { setV("channel:${channelId}", channel) }
            channel
        }
    }

    override suspend fun updateChannel(update: Channel) = tm.useTransaction { db ->
        val channelCol = db.getCollection<Channel>()
        val userCol = db.getCollection<User>()

        userCol.findOne(User::userId eq update.owner)
            ?: throw BadRequestException("The specified owner does not exist")

        val old = channelCol.findOne(Channel::channelId eq update.channelId)
        channelCol.updateOne(update)

        redis.use {
            setV("channel:${update.channelId}", update)
            setRemove("channels", old)
            setAdd("channels", update)
        }

        update
    }

    override suspend fun deleteChannel(channel: Channel) = tm.use { db ->
        val col = db.getCollection<Channel>()
        col.deleteOne(Channel::channelId eq channel.channelId)

        redis.use {
            del("channel:${channel.channelId}")
            del("channel_users:${channel.channelId}")
            setRemove("channels", channel)
            keys("user_channels:*").collect {
                setRemove(it, channel)
            }
        }

        Unit
    }

    override suspend fun isUserInChannel(channel: Channel, user: User): Boolean {
        val cache = redis.use { setMembers<User>("channel_users:${channel.channelId}") }
        if (!cache.isNullOrEmpty())
            return cache.find { it.userId == user.userId } != null

        return tm.use { db ->
            val usersChannelCol = db.getCollection<UserChannel>()
            val res = usersChannelCol.findOne(and(
                UserChannel::channel eq channel.channelId,
                UserChannel::user eq user.userId
            ))

            if (res != null) {
                redis.use { setAdd("channel_users:${channel.channelId}", user) }
                true
            } else {
                false
            }
        }
    }

    override suspend fun getChannelMembers(channel: Channel, pagination: Pagination): Set<User> {
        val cache = redis.use { setMembers<User>("channel_users:${channel.channelId}") }
        if (!cache.isNullOrEmpty()) {
            return cache.drop(pagination.offset)
                .take(pagination.limit)
                .toSet()
        }

        return tm.useTransaction { db ->
            val userChannelCol = db.getCollection<UserChannel>()
            val userCol = db.getCollection<User>()

            val results = userChannelCol.find(UserChannel::channel eq channel.channelId)
                .toList()
                .mapNotNull { userCol.findOne(User::userId eq it.user) }

            if (results.isNotEmpty())
                redis.use { setAdd("channel_users:${channel.channelId}", *results.toTypedArray()) }

            results.drop(pagination.offset)
                .take(pagination.limit)
                .toSet()
        }
    }

    override suspend fun addChannelMember(channel: Channel, username: String) = tm.useTransaction { db ->
        val user = findUser(username)
        if (isUserInChannel(channel, user))
            throw BadRequestException("The specified user is already subscribed to the channel")

        val userChannelCol = db.getCollection<UserChannel>()
        userChannelCol.insertOne(UserChannel(channel.channelId, user.userId))

        redis.use {
            setAdd("user_channels:$username", channel)
            setAdd("channel_users:${channel.channelId}", user)
        }

        Unit
    }

    override suspend fun removeChannelMember(channel: Channel, username: String) = tm.useTransaction { db ->
        val user = findUser(username)
        if (!isUserInChannel(channel, user))
            throw BadRequestException("The specified user is not subscribed to the channel")

        if (user.userId == channel.owner)
            throw BadRequestException("You cannot be removed from this channel since you are the owner")

        val userChannelCol = db.getCollection<UserChannel>()
        userChannelCol.deleteOne(and(
            UserChannel::channel eq channel.channelId,
            UserChannel::user eq user.userId
        ))

        redis.use {
            setRemove("user_channels:$username", channel)
            setRemove("channel_users:${channel.channelId}", user)
        }

        Unit
    }

    override suspend fun getChannelMessages(channel: Channel, pagination: Pagination): Set<ChannelMessage> {
        val cached = redis.use { zSetRange<ChannelMessage>("channel_messages:${channel.channelId}", 0, -1) }
        if (!cached.isNullOrEmpty()) {
            return cached.reversed()
                .drop(pagination.offset)
                .take(pagination.limit)
                .toSet()
        }

        return tm.use { db ->
            val col = db.getCollection<ChannelMessage>()
            val messages = col.find(ChannelMessage::channelId eq channel.channelId)
                .toList()

            redis.use {
                val scoredMessages = messages.map {
                    ScoredValue.just(it.createdAt.toDouble(), it)
                }

                if (scoredMessages.isNotEmpty())
                    zSetAdd("channel_messages:${channel.channelId}", *scoredMessages.toTypedArray())
            }

            messages.toSet()
        }
    }

    override suspend fun createChannelMessage(channel: Channel, message: ChannelMessage) = tm.use { db ->
        val messagesCol = db.getCollection<ChannelMessage>()
        val mediaCol = db.getCollection<Media>()

        if (message.media != null) {
            val cachedMedia = redis.use { getV<Media>("media:${message.media}") }
            if (cachedMedia == null) {
                val media = mediaCol.findOne(Media::blobName eq message.media)
                    ?: throw BadRequestException("Invalid message media blob")

                redis.use { setV("media:${message.media}", media) }
            }
        }

        if (message.replyTo != null) {
            val cachedReplyTo = redis.use { getV<ChannelMessage>("message:${message.replyTo}") }
            if (cachedReplyTo == null) {
                val replyTo = messagesCol.findOne(ChannelMessage::messageId eq message.replyTo)
                    ?: throw BadRequestException("The message you're replying to does not exist")

                redis.use { setV("message:${message.replyTo}", replyTo) }
            }
        }

        messagesCol.insertOne(message)

        redis.use {
            setV("message:${message.messageId}", message)
            zSetAdd(
                "channel_messages:${channel.channelId}",
                ScoredValue.just(message.createdAt.toDouble(), message)
            )
        }

        message
    }

    override suspend fun getChannelMessage(channel: Channel, messageId: String): ChannelMessage {
        val cached = redis.use { getV<ChannelMessage>("message:${messageId}") }
        if (cached != null)
            return cached

        return tm.use { db ->
            val col = db.getCollection<ChannelMessage>()
            val message = col.findOne(ChannelMessage::messageId eq messageId)
                ?: throw NotFoundException()

            redis.use { setV("message:${messageId}", message) }
            message
        }
    }

    override suspend fun deleteChannelMessage(channel: Channel, message: ChannelMessage) = tm.use { db ->
        val messagesCol = db.getCollection<ChannelMessage>()
        val mediaCol = db.getCollection<Media>()

        messagesCol.deleteOne(ChannelMessage::messageId eq message.messageId)
        if (message.media != null) {
            mediaCol.deleteOne(Media::blobName eq message.media)
            redis.use { del("media:${message.media}") }
        }

        redis.use {
            del("message:${message.messageId}")
            zSetRemove("channel_messages:${channel.channelId}", message)
        }

        Unit
    }

    private suspend fun findUser(username: String): User {
        val cached = redis.use { getV<User>("user:$username") }
        if (cached != null)
            return cached

        return tm.use { db ->
            val userCol = db.getCollection<User>()
            val user = userCol.findOne(User::nickname eq username)
                ?: throw BadRequestException("The specified user does not exist")

            redis.use { setV("user:$username", user) }
            user
        }
    }

}