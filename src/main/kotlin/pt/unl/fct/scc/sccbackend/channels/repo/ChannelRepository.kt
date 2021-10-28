package pt.unl.fct.scc.sccbackend.channels.repo

import pt.unl.fct.scc.sccbackend.channels.model.Channel
import pt.unl.fct.scc.sccbackend.users.model.User

interface ChannelRepository {

    suspend fun createChannel(channel: Channel): Channel

    suspend fun getChannel(channelId: String): Channel

    suspend fun updateChannel(update: Channel): Channel

    suspend fun deleteChannel(channel: Channel)

    suspend fun isUserInChannel(user: User, channel: Channel): Boolean

}