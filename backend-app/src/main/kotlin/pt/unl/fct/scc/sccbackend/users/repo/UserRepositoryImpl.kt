package pt.unl.fct.scc.sccbackend.users.repo

import kotlinx.coroutines.flow.collect
import org.litote.kmongo.coroutine.updateOne
import org.litote.kmongo.eq
import org.springframework.stereotype.Repository
import pt.unl.fct.scc.sccbackend.channels.model.Channel
import pt.unl.fct.scc.sccbackend.common.BadRequestException
import pt.unl.fct.scc.sccbackend.common.ConflictException
import pt.unl.fct.scc.sccbackend.common.NotFoundException
import pt.unl.fct.scc.sccbackend.common.cache.*
import pt.unl.fct.scc.sccbackend.common.database.KMongoTM
import pt.unl.fct.scc.sccbackend.common.pagination.Pagination
import pt.unl.fct.scc.sccbackend.media.model.Media
import pt.unl.fct.scc.sccbackend.users.model.User
import pt.unl.fct.scc.sccbackend.users.model.UserChannel
import pt.unl.fct.scc.sccbackend.users.model.toDeletedUser

@Repository
class UserRepositoryImpl(
    val tm: KMongoTM,
    val redis: RedisClientProvider
) : UserRepository {

    override suspend fun createUser(user: User): User {
        if (redis.fetch { getV<User>("user:${user.nickname}") } != null)
            throw ConflictException("The user ${user.nickname} already exists")

        return tm.useTransaction { db ->
            val userCol = db.getCollection<User>()

            val existingUser = userCol.findOne(User::nickname eq user.nickname)
            if (existingUser != null) {
                redis.run { setV("user:${user.nickname}", existingUser) }
                throw ConflictException("The user ${user.nickname} already exists")
            }

            userCol.insertOne(user)

            redis.run { setV("user:${user.nickname}", user) }

            user
        }
    }

    override suspend fun getUser(username: String): User {
        val cached = redis.fetch { getV<User>("user:$username") }
        if (cached != null)
            return cached

        return tm.use { db ->
            val col = db.getCollection<User>()
            val user = col.findOne(User::nickname eq username)
                ?: throw NotFoundException()

            redis.run { setV("user:$username", user) }
            user
        }
    }

    override suspend fun updateUser(update: User) = tm.useTransaction { db ->
        if (update.photo != null) {
            if (redis.fetch { getV<Media>("media:${update.photo}") } == null) {
                val mediaCol = db.getCollection<Media>()
                val media = mediaCol.findOne(Media::mediaId eq update.photo)
                    ?: throw BadRequestException("The specified user photo does not exist")

                redis.run { setV("media:${update.photo}", media) }
            }
        }

        val userCol = db.getCollection<User>()
        userCol.updateOne(update)

        redis.run { setV("user:${update.nickname}", update) }

        update
    }

    override suspend fun deleteUser(user: User) = tm.useTransaction { db ->
        val userCol = db.getCollection<User>()
        val channelCol = db.getCollection<Channel>()

        val count = channelCol.countDocuments(Channel::owner eq user.userId)
        if (count != 0L)
            throw BadRequestException("The user cannot be deleted because it owns channels")

        userCol.updateOne(user.toDeletedUser())

        redis.run {
            del("user:${user.nickname}")
            del("user_channels:${user.userId}")
            keys("channel_users:*").collect {
                setRemove(it, user)
            }

            if (user.photo != null)
                del("media:${user.photo}")
        }

        Unit
    }

    override suspend fun getUserChannels(user: User, pagination: Pagination): Set<Channel> {
        val channel = redis.fetch { setMembers<Channel>("user_channels:${user.userId}") }
        if (!channel.isNullOrEmpty()) {
            return channel.drop(pagination.offset)
                .take(pagination.limit)
                .toSet()
        }

        return tm.use { db ->
            val userChannelCol = db.getCollection<UserChannel>()
            val channelCol = db.getCollection<Channel>()

            val result = userChannelCol.find(UserChannel::user eq user.userId)
                .toList()
                .mapNotNull { channelCol.findOne(Channel::channelId eq it.channel) }

            if (result.isNotEmpty())
                redis.run { setAdd("user_channels:${user.userId}", *result.toTypedArray()) }

            result.drop(pagination.offset)
                .take(pagination.limit)
                .toSet()
        }
    }

}