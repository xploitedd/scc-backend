package pt.unl.fct.scc.sccbackend.channels.repo

import pt.unl.fct.scc.sccbackend.channels.model.Channel
import pt.unl.fct.scc.sccbackend.channels.model.ChannelMessage
import pt.unl.fct.scc.sccbackend.common.pagination.Pagination
import pt.unl.fct.scc.sccbackend.users.model.User

interface ChannelRepository {

    suspend fun getChannels(user: User?, pagination: Pagination): Set<Channel>

    suspend fun getTrendingChannels(user: User?, pagination: Pagination): Set<Channel>

    suspend fun createChannel(channel: Channel): Channel

    suspend fun getChannel(channelId: String): Channel

    suspend fun updateChannel(update: Channel): Channel

    suspend fun deleteChannel(channel: Channel)

    suspend fun isUserInChannel(channel: Channel, user: User): Boolean

    suspend fun getChannelMembers(channel: Channel, pagination: Pagination): Set<User>

    suspend fun addChannelMember(channel: Channel, username: String)

    suspend fun removeChannelMember(channel: Channel, username: String)

    suspend fun getChannelMessages(channel: Channel, pagination: Pagination): Set<ChannelMessage>

    suspend fun searchChannelMessages(channel: Channel, query: String, pagination: Pagination): Set<ChannelMessage>

    suspend fun createChannelMessage(channel: Channel, message: ChannelMessage): ChannelMessage

    suspend fun getChannelMessage(channel: Channel, messageId: String): ChannelMessage

    suspend fun deleteChannelMessage(channel: Channel, message: ChannelMessage)

}