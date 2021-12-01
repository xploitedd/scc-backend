package pt.unl.fct.scc.sccbackend.channels.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.litote.kmongo.newId
import pt.unl.fct.scc.sccbackend.users.model.User

@Serializable
data class ChannelCreateInput(
    val name: String,
    val private: Boolean
)

@Serializable
data class ChannelUpdateInput(
    val name: String? = null,
    val owner: String? = null,
    val private: Boolean? = null,
)

@Serializable
data class ChannelMemberInput(
    val username: String
)

@Serializable
data class Channel(
    val name: String,
    val owner: String,
    val private: Boolean,
    @SerialName("_id")
    val channelId: String = newId<Channel>().toString(),
    val deleted: Boolean = false
)

@Serializable
data class TrendingChannel(
    val channelId: String,
    val msgCount: Long
)

@Serializable
data class ChannelReducedDto(
    val channelId: String,
    val name: String,
    val owner: String,
    val private: Boolean
)

fun Channel.toReducedDto() = ChannelReducedDto(
    channelId,
    name,
    owner,
    private
)

fun ChannelCreateInput.toChannel(user: User) = Channel(
    name,
    user.nickname,
    private
)

fun ChannelUpdateInput.toChannel(channel: Channel) = Channel(
    name ?: channel.name,
    owner ?: channel.owner,
    private ?: channel.private,
    channel.channelId
)