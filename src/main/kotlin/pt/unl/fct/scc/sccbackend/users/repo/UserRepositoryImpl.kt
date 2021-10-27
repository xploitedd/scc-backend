package pt.unl.fct.scc.sccbackend.users.repo

import org.litote.kmongo.coroutine.updateOne
import org.litote.kmongo.eq
import org.springframework.stereotype.Repository
import pt.unl.fct.scc.sccbackend.channels.model.Channel
import pt.unl.fct.scc.sccbackend.channels.model.ChannelReducedDto
import pt.unl.fct.scc.sccbackend.channels.model.toReducedDto
import pt.unl.fct.scc.sccbackend.common.ConflictException
import pt.unl.fct.scc.sccbackend.common.NotFoundException
import pt.unl.fct.scc.sccbackend.common.database.KMongoTM
import pt.unl.fct.scc.sccbackend.users.model.User
import pt.unl.fct.scc.sccbackend.users.model.UserChannel

@Repository
class UserRepositoryImpl(val tm: KMongoTM) : UserRepository {
    override suspend fun createUser(user: User) = tm.useTransaction { db ->
        val col = db.getCollection<User>()
        val existing = col.findOne(User::nickname eq user.nickname)

        if (existing != null)
            throw ConflictException("The user ${user.nickname} already exists")

        col.insertOne(user)
        user
    }

    override suspend fun getUser(username: String): User {
        val col = tm.database.getCollection<User>()
        return col.findOne(User::nickname eq username)
            ?: throw NotFoundException()
    }

    override suspend fun updateUser(update: User): User {
        val col = tm.database.getCollection<User>()
        col.updateOne(update)
        return update
    }

    override suspend fun deleteUser(user: User) {
        val col = tm.database.getCollection<User>()
        col.deleteOne(User::userId eq user.userId)
    }

    override suspend fun getUserChannels(user: User): List<ChannelReducedDto> {
        val userChannelCol = tm.database.getCollection<UserChannel>()
        val channelCol = tm.database.getCollection<Channel>()
        val list = mutableListOf<ChannelReducedDto>()

        userChannelCol.find(UserChannel::user eq user.userId).consumeEach {
            val channel = channelCol.findOne(Channel::channelId eq it.channel)
            if (channel != null)
                list.add(channel.toReducedDto())
        }

        return list
    }
}