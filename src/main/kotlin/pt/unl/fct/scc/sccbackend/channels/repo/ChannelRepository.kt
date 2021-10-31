package pt.unl.fct.scc.sccbackend.channels.repo

import pt.unl.fct.scc.sccbackend.channels.model.Channel
import pt.unl.fct.scc.sccbackend.common.pagination.Pagination
import pt.unl.fct.scc.sccbackend.users.model.User

interface ChannelRepository {

    suspend fun getChannels(user: User?, pagination: Pagination): Set<Channel>

    suspend fun createChannel(channel: Channel): Channel

    suspend fun getChannel(channelId: String): Channel

    suspend fun updateChannel(update: Channel): Channel

    suspend fun deleteChannel(channel: Channel)

    suspend fun isUserInChannel(channel: Channel, user: User): Boolean

    suspend fun getChannelMembers(channel: Channel, pagination: Pagination): Set<User>

    suspend fun addChannelMember(channel: Channel, username: String)

    suspend fun removeChannelMember(channel: Channel, username: String)

}