package pt.unl.fct.scc.sccbackend.users.repo

import org.litote.kmongo.coroutine.updateOne
import org.litote.kmongo.eq
import org.springframework.stereotype.Repository
import pt.unl.fct.scc.sccbackend.channels.model.Channel
import pt.unl.fct.scc.sccbackend.common.BadRequestException
import pt.unl.fct.scc.sccbackend.common.ConflictException
import pt.unl.fct.scc.sccbackend.common.NotFoundException
import pt.unl.fct.scc.sccbackend.common.database.KMongoTM
import pt.unl.fct.scc.sccbackend.common.pagination.Pagination
import pt.unl.fct.scc.sccbackend.media.model.Media
import pt.unl.fct.scc.sccbackend.users.model.User
import pt.unl.fct.scc.sccbackend.users.model.UserChannel

@Repository
class UserRepositoryImpl(val tm: KMongoTM) : UserRepository {

    override suspend fun createUser(user: User) = tm.useTransaction { db ->
        val userCol = db.getCollection<User>()
        val mediaCol = db.getCollection<Media>()

        val existingUser = userCol.findOne(User::nickname eq user.nickname)
        if (existingUser != null)
            throw ConflictException("The user ${user.nickname} already exists")

        mediaCol.findOne(Media::blobName eq user.photo)
            ?: throw BadRequestException("The specified photo does not exist")

        userCol.insertOne(user)
        user
    }

    override suspend fun getUser(username: String) = tm.use { db ->
        val col = db.getCollection<User>()
        col.findOne(User::nickname eq username)
            ?: throw NotFoundException()
    }

    override suspend fun updateUser(update: User) = tm.use { db ->
        val col = db.getCollection<User>()
        col.updateOne(update)
        getUser(update.nickname)
    }

    override suspend fun deleteUser(user: User) = tm.use { db ->
        val userCol = db.getCollection<User>()
        val mediaCol = db.getCollection<Media>()
        val channelCol = db.getCollection<Channel>()

        val count = channelCol.countDocuments(Channel::owner eq user.userId)
        if (count != 0L)
            throw BadRequestException("The user cannot be deleted because it owns channels")

        mediaCol.deleteOne(Media::blobName eq user.photo)
        userCol.deleteOne(User::userId eq user.userId)
        Unit
    }

    override suspend fun getUserChannels(user: User, pagination: Pagination) = tm.use { db ->
        val userChannelCol = db.getCollection<UserChannel>()
        val channelCol = db.getCollection<Channel>()

        userChannelCol.find(UserChannel::user eq user.userId)
            .skip(pagination.offset)
            .limit(pagination.limit)
            .toList()
            .mapNotNull { channelCol.findOne(Channel::channelId eq it.channel) }
            .toSet()
    }

}