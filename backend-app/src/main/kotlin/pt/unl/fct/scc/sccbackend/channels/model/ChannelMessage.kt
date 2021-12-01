package pt.unl.fct.scc.sccbackend.channels.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.litote.kmongo.newId
import pt.unl.fct.scc.sccbackend.users.model.User
import java.time.Instant

@Serializable
data class ChannelMessageInput(
    val text: String,
    val media: String? = null,
    val replyTo: String? = null
)

@Serializable
data class ChannelMessage(
    val channelId: String,
    val user: String,
    val text: String,
    val media: String? = null,
    val replyTo: String? = null,
    val createdAt: Long = Instant.now().toEpochMilli(),
    @SerialName("_id")
    val messageId: String = newId<ChannelMessage>().toString(),
    val deleted: Boolean = false
)

@Serializable
data class ChannelMessageReducedDto(
    val messageId: String,
    val user: String,
    val text: String,
    val timestamp: Long,
    val media: String? = null,
    val replyTo: String? = null
)

fun ChannelMessageInput.toChannelMessage(channel: Channel, user: User) = ChannelMessage(
    channel.channelId,
    user.nickname,
    text,
    media,
    replyTo
)

fun ChannelMessage.toReducedDto() = ChannelMessageReducedDto(
    messageId,
    user,
    text,
    createdAt,
    media,
    replyTo
)