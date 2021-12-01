package pt.unl.fct.scc.sccbackend.channels.repo

import io.lettuce.core.ScoredValue
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import org.litote.kmongo.and
import org.litote.kmongo.coroutine.updateOne
import org.litote.kmongo.eq
import org.litote.kmongo.setValue
import org.litote.kmongo.text
import org.litote.kmongo.sortByMetaTextScore
import org.springframework.stereotype.Repository
import pt.unl.fct.scc.sccbackend.channels.model.Channel
import pt.unl.fct.scc.sccbackend.channels.model.ChannelMessage
import pt.unl.fct.scc.sccbackend.channels.model.TrendingChannel
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
        val cached = redis.fetch { setMembers<Channel>("channels") }
        if (!cached.isNullOrEmpty()) {
            return cached.toList()
                .retrieve(user, pagination)
        }

        return tm.use { db ->
            val col = db.getCollection<Channel>()
            val channels = col.find(Channel::deleted eq false)
                .toList()

            if (channels.isNotEmpty())
                redis.run { setAdd("channels", *channels.toTypedArray()) }

            channels.retrieve(user, pagination)
        }
    }

    override suspend fun getTrendingChannels(user: User?, pagination: Pagination): Set<Channel> {
        val cached = redis.fetch { setMembers<Channel>("trending_channels") }
        if (!cached.isNullOrEmpty()) {
            return cached.toList()
                .retrieve(user, pagination)
        }

        return tm.use { db ->
            val channelCol = db.getCollection<Channel>()
            val trending = db.getCollection<TrendingChannel>()
                .find()
                .toFlow()
                .mapNotNull {
                    channelCol.findOne(Channel::channelId eq it.channelId)
                }
                .toList()

            if (trending.isNotEmpty())
                redis.run { setAdd("trending_channels", *trending.toTypedArray()) }

            trending.retrieve(user, pagination)
        }
    }

    override suspend fun createChannel(channel: Channel) = tm.use { db ->
        val col = db.getCollection<Channel>()
        col.insertOne(channel)

        redis.run {
            setV("channel:${channel.channelId}", channel)
            setAdd("channels", channel)
        }

        channel
    }

    override suspend fun getChannel(channelId: String): Channel {
        val cached = redis.fetch { getV<Channel>("channel:${channelId}") }
        if (cached != null)
            return cached

        return tm.use { db ->
            val col = db.getCollection<Channel>()
            val channel = col.findOne(and(Channel::channelId eq channelId, Channel::deleted eq false))
                ?: throw NotFoundException()

            redis.run { setV("channel:${channelId}", channel) }
            channel
        }
    }

    override suspend fun updateChannel(update: Channel) = tm.useTransaction { db ->
        val channelCol = db.getCollection<Channel>()
        val userCol = db.getCollection<User>()

        val old = channelCol.findOne(Channel::channelId eq update.channelId)!!
        val newOwner = userCol.findOne(User::nickname eq update.owner)
            ?: throw BadRequestException("The specified owner does not exist")

        val hasNewOwner = old.owner != update.owner
        if (hasNewOwner) {
            val userChannelCol = db.getCollection<UserChannel>()
            userChannelCol.insertOne(UserChannel(update.channelId, newOwner.userId))
        }

        channelCol.updateOne(update)

        redis.run {
            setV("channel:${update.channelId}", update)
            setRemove("channels", old)
            setAdd("channels", update)

            if (hasNewOwner)
                setAdd("channel_users:${update.channelId}", newOwner)

            keys("user_channels:*").collect {
                setRemove(it, old)
                setAdd(it, update)
            }
        }

        update
    }

    override suspend fun deleteChannel(channel: Channel) = tm.useTransaction { db ->
        val channelCol = db.getCollection<Channel>()
        val userChannelCol = db.getCollection<UserChannel>()

        channelCol.updateOne(Channel::channelId eq channel.channelId, setValue(Channel::deleted, true))
        userChannelCol.deleteMany(UserChannel::channel eq channel.channelId)

        redis.run {
            del("channel:${channel.channelId}")
            del("channel_users:${channel.channelId}")
            setRemove("channels", channel)
            setRemove("trending_channels", channel)
            keys("user_channels:*").collect {
                setRemove(it, channel)
            }
        }

        Unit
    }

    override suspend fun isUserInChannel(channel: Channel, user: User): Boolean {
        val cache = redis.fetch { setMembers<User>("channel_users:${channel.channelId}") }
        if (!cache.isNullOrEmpty())
            return cache.find { it.userId == user.userId } != null

        return tm.use { db ->
            val usersChannelCol = db.getCollection<UserChannel>()
            val res = usersChannelCol.findOne(and(
                UserChannel::channel eq channel.channelId,
                UserChannel::user eq user.userId
            ))

            if (res != null) {
                redis.run { setAdd("channel_users:${channel.channelId}", user) }
                true
            } else {
                false
            }
        }
    }

    override suspend fun getChannelMembers(channel: Channel, pagination: Pagination): Set<User> {
        val cache = redis.fetch { setMembers<User>("channel_users:${channel.channelId}") }
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
                redis.run { setAdd("channel_users:${channel.channelId}", *results.toTypedArray()) }

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

        redis.run {
            setAdd("user_channels:${user.userId}", channel)
            setAdd("channel_users:${channel.channelId}", user)
        }

        Unit
    }

    override suspend fun removeChannelMember(channel: Channel, username: String) = tm.useTransaction { db ->
        val user = findUser(username)
        if (!isUserInChannel(channel, user))
            throw BadRequestException("The specified user is not subscribed to the channel")

        if (user.nickname == channel.owner)
            throw BadRequestException("You cannot be removed from this channel since you are the owner")

        val userChannelCol = db.getCollection<UserChannel>()
        userChannelCol.deleteOne(and(
            UserChannel::channel eq channel.channelId,
            UserChannel::user eq user.userId
        ))

        redis.run {
            setRemove("user_channels:${user.userId}", channel)
            setRemove("channel_users:${channel.channelId}", user)
        }

        Unit
    }

    override suspend fun getChannelMessages(channel: Channel, pagination: Pagination): Set<ChannelMessage> {
        val cached = redis.fetch { zSetRange<ChannelMessage>("channel_messages:${channel.channelId}", 0, -1) }
        if (!cached.isNullOrEmpty()) {
            return cached.reversed()
                .drop(pagination.offset)
                .take(pagination.limit)
                .toSet()
        }

        return tm.use { db ->
            val col = db.getCollection<ChannelMessage>()
            val messages = col.find(and(ChannelMessage::channelId eq channel.channelId, ChannelMessage::deleted eq false))
                .toList()

            redis.run {
                val scoredMessages = messages.map {
                    ScoredValue.just(it.createdAt.toDouble(), it)
                }

                if (scoredMessages.isNotEmpty())
                    zSetAdd("channel_messages:${channel.channelId}", *scoredMessages.toTypedArray())
            }

            messages.toSet()
        }
    }

    override suspend fun searchChannelMessages(channel: Channel, query: String, pagination: Pagination) = tm.use { db ->
        val col = db.getCollection<ChannelMessage>()
        col.find(text(query))
            .sort(ChannelMessage::text.sortByMetaTextScore())
            .limit(pagination.limit)
            .skip(pagination.offset)
            .toList()
            .toSet()
    }

    override suspend fun createChannelMessage(channel: Channel, message: ChannelMessage) = tm.use { db ->
        val messagesCol = db.getCollection<ChannelMessage>()
        val mediaCol = db.getCollection<Media>()

        if (message.media != null) {
            val cachedMedia = redis.fetch { getV<Media>("media:${message.media}") }
            if (cachedMedia == null) {
                val media = mediaCol.findOne(Media::mediaId eq message.media)
                    ?: throw BadRequestException("Invalid message media blob")

                redis.run { setV("media:${message.media}", media) }
            }
        }

        if (message.replyTo != null) {
            val cachedReplyTo = redis.fetch { getV<ChannelMessage>("message:${message.replyTo}") }
            if (cachedReplyTo == null) {
                val replyTo = messagesCol.findOne(ChannelMessage::messageId eq message.replyTo)
                    ?: throw BadRequestException("The message you're replying to does not exist")

                redis.run { setV("message:${message.replyTo}", replyTo) }
            }
        }

        messagesCol.insertOne(message)

        redis.run {
            setV("message:${message.messageId}", message)
            zSetAdd(
                "channel_messages:${channel.channelId}",
                ScoredValue.just(message.createdAt.toDouble(), message)
            )
        }

        message
    }

    override suspend fun getChannelMessage(channel: Channel, messageId: String): ChannelMessage {
        val cached = redis.fetch { getV<ChannelMessage>("message:${messageId}") }
        if (cached != null)
            return cached

        return tm.use { db ->
            val col = db.getCollection<ChannelMessage>()
            val message = col.findOne(and(ChannelMessage::messageId eq messageId, ChannelMessage::deleted eq false))
                ?: throw NotFoundException()

            redis.run { setV("message:${messageId}", message) }
            message
        }
    }

    override suspend fun deleteChannelMessage(channel: Channel, message: ChannelMessage) = tm.use { db ->
        val messagesCol = db.getCollection<ChannelMessage>()

        messagesCol.updateOne(ChannelMessage::messageId eq message.messageId, setValue(ChannelMessage::deleted, true))
        if (message.media != null)
            redis.run { del("media:${message.media}") }

        redis.run {
            del("message:${message.messageId}")
            zSetRemove("channel_messages:${channel.channelId}", message)
        }

        Unit
    }

    private suspend fun findUser(username: String): User {
        val cached = redis.fetch { getV<User>("user:$username") }
        if (cached != null)
            return cached

        return tm.use { db ->
            val userCol = db.getCollection<User>()
            val user = userCol.findOne(User::nickname eq username)
                ?: throw BadRequestException("The specified user does not exist")

            redis.run { setV("user:$username", user) }
            user
        }
    }

    private suspend fun List<Channel>.retrieve(user: User?, pagination: Pagination): Set<Channel> {
        return filter { !it.private || (user != null && isUserInChannel(it, user)) }
            .drop(pagination.offset)
            .take(pagination.limit)
            .toSet()
    }

}