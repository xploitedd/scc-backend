package pt.unl.fct.scc.sccbackend.channels.repo

import org.litote.kmongo.and
import org.litote.kmongo.coroutine.updateOne
import org.litote.kmongo.eq
import org.litote.kmongo.or
import org.springframework.stereotype.Repository
import pt.unl.fct.scc.sccbackend.channels.model.Channel
import pt.unl.fct.scc.sccbackend.channels.model.ChannelNewMemberInput
import pt.unl.fct.scc.sccbackend.common.BadRequestException
import pt.unl.fct.scc.sccbackend.common.NotFoundException
import pt.unl.fct.scc.sccbackend.common.database.KMongoTM
import pt.unl.fct.scc.sccbackend.users.model.User
import pt.unl.fct.scc.sccbackend.users.model.UserChannel

@Repository
class ChannelRepositoryImpl(val tm: KMongoTM) : ChannelRepository {

    override suspend fun getChannels(user: User?) = tm.use { db ->
        val col = db.getCollection<Channel>()
        val channels = col.find(or(
            Channel::private eq false, Channel::owner eq user?.userId
        ))

        channels.toList()
    }

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

    override suspend fun isUserInChannel(channel: Channel, user: User) = tm.use { db ->
        val usersChannelCol = db.getCollection<UserChannel>()
        val res = usersChannelCol.findOne(and(
            UserChannel::channel eq channel.channelId,
            UserChannel::user eq user.userId
        ))

        res != null
    }

    override suspend fun getChannelMembers(channel: Channel) = tm.useTransaction { db ->
        val userChannelCol = db.getCollection<UserChannel>()
        val userCol = db.getCollection<User>()

        userChannelCol.find(UserChannel::channel eq channel.channelId)
            .toList()
            .mapNotNull { userCol.findOne(User::userId eq it.user) }
    }

    override suspend fun addChannelMember(channel: Channel, input: ChannelNewMemberInput) = tm.useTransaction { db ->
        val userCol = db.getCollection<User>()
        val user = userCol.findOne(User::userId eq input.userId)
            ?: throw BadRequestException("The specified User Id is invalid")

        subscribeToChannel(channel, user)
    }

    override suspend fun subscribeToChannel(channel: Channel, user: User) = tm.use { db ->
        val userChannelCol = db.getCollection<UserChannel>()
        userChannelCol.insertOne(UserChannel(
            channel.channelId,
            user.userId
        ))

        Unit
    }


}