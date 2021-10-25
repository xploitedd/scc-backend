package pt.unl.fct.scc.sccbackend.channels.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Channel(
    @SerialName("_id") val name: String,
    val owner: String,
    val private: Boolean,
    val messages: List<ChannelMessage>
)

@Serializable
data class ChannelMessage(
    @SerialName("_id") val messageId: String,
    val user: String,
    val text: String,
    val media: String,
    val replyTo: String
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