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
    val name: String?,
    val owner: String?,
    val private: Boolean?,
)

@Serializable
data class Channel(
    val name: String,
    val owner: String,
    val private: Boolean,
    val messages: List<ChannelMessage> = emptyList(),
    @SerialName("_id")
    val channelId: String = newId<Channel>().toString()
)

@Serializable
data class ChannelMessage(
    val user: String,
    val text: String,
    val media: String?,
    val replyTo: String?,
    @SerialName("_id")
    val messageId: String = newId<ChannelMessage>().toString(),
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
    user.userId,
    private
)

fun ChannelUpdateInput.toChannel(channel: Channel) = Channel(
    name ?: channel.name,
    owner ?: channel.owner,
    private ?: channel.private,
    channel.messages,
    channel.channelId
)