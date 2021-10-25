package pt.unl.fct.scc.sccbackend.users.repo

import org.litote.kmongo.eq
import org.springframework.stereotype.Repository
import pt.unl.fct.scc.sccbackend.channels.model.Channel
import pt.unl.fct.scc.sccbackend.channels.model.ChannelReducedDto
import pt.unl.fct.scc.sccbackend.channels.model.toReducedDto
import pt.unl.fct.scc.sccbackend.common.database.KMongoTM
import pt.unl.fct.scc.sccbackend.users.model.User
import pt.unl.fct.scc.sccbackend.users.model.UserChannel

@Repository
class UserRepositoryImpl(val tm: KMongoTM) : UserRepository {
    override suspend fun createUser(user: User) = tm.useTransaction { db ->
        val col = db.getCollection<User>()
        val existing = col.findOne(User::nickname eq user.nickname)

        if (existing != null)
            throw Exception("The user ${user.nickname} already exists")

        col.insertOne(user)
        user
    }

    override suspend fun updateUser(user: User): User {
        TODO("Not yet implemented")
    }

    override suspend fun deleteUser(user: User) {
        val col = tm.database.getCollection<User>()
        col.deleteOne(User::nickname eq user.nickname)
    }

    override suspend fun getUserChannels(user: User): List<ChannelReducedDto> {
        val userChannelCol = tm.database.getCollection<UserChannel>()
        val channelCol = tm.database.getCollection<Channel>()
        val list = mutableListOf<ChannelReducedDto>()

        userChannelCol.find(UserChannel::user eq user.nickname).consumeEach {
            val channel = channelCol.findOne(Channel::name eq it.channel)
            // TODO: Delete channel from user channels if == null
            if (channel != null)
                list.add(channel.toReducedDto())
        }

        return list
    }
}