package pt.unl.fct.scc.sccbackend.channels.repo

import org.litote.kmongo.and
import org.litote.kmongo.coroutine.updateOne
import org.litote.kmongo.eq
import org.springframework.stereotype.Repository
import pt.unl.fct.scc.sccbackend.channels.model.Channel
import pt.unl.fct.scc.sccbackend.common.BadRequestException
import pt.unl.fct.scc.sccbackend.common.NotFoundException
import pt.unl.fct.scc.sccbackend.common.database.KMongoTM
import pt.unl.fct.scc.sccbackend.users.model.User
import pt.unl.fct.scc.sccbackend.users.model.UserChannel

@Repository
class ChannelRepositoryImpl(val tm: KMongoTM) : ChannelRepository {

    override suspend fun createChannel(channel: Channel) = tm.use { db ->
        val col = db.getCollection<Channel>()
        col.insertOne(channel)
        channel
    }

    override suspend fun getChannel(channelId: String) = tm.use { db ->
        val col = db.getCollection<Channel>()
        col.findOne(Channel::channelId eq channelId)
            ?: throw NotFoundException()
    }

    override suspend fun updateChannel(update: Channel) = tm.useTransaction { db ->
        val channelCol = db.getCollection<Channel>()
        val userCol = db.getCollection<User>()

        userCol.findOne(User::userId eq update.owner)
            ?: throw BadRequestException("The specified owner does not exist")

        channelCol.updateOne(update)
        getChannel(update.channelId)
    }

    override suspend fun deleteChannel(channel: Channel) = tm.use { db ->
        val col = db.getCollection<Channel>()
        col.deleteOne(Channel::channelId eq channel.channelId)
        Unit
    }

    override suspend fun isUserInChannel(user: User, channel: Channel) = tm.use { db ->
        val usersChannelCol = db.getCollection<UserChannel>()
        val res = usersChannelCol.findOne(and(
            UserChannel::channel eq channel.channelId,
            UserChannel::user eq user.userId
        ))

        res != null
    }

}