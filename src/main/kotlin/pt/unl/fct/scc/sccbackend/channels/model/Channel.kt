package pt.unl.fct.scc.sccbackend.channels.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.litote.kmongo.newId

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
    val name: String,
    val owner: String,
    val private: Boolean
)

fun Channel.toReducedDto() = ChannelReducedDto(
    name,
    owner,
    private
)