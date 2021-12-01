package pt.unl.fct.scc.sccbackend.channels

import org.springframework.web.util.UriTemplate

object ChannelUri {

    const val CHANNELS = "/channels"
    const val CHANNELS_TRENDING = "/channels/trending"
    const val CHANNEL = "$CHANNELS/{channelId}"
    const val CHANNEL_MEMBERS = "$CHANNEL/members"
    const val CHANNEL_MESSAGES = "$CHANNEL/messages"
    const val CHANNEL_MESSAGES_SEARCH = "$CHANNEL_MESSAGES/search"
    const val CHANNEL_MESSAGE = "$CHANNEL_MESSAGES/{messageId}"

    fun forChannel(channelId: String) =
        UriTemplate(CHANNEL).expand(channelId)

    fun forChannelMessage(channelId: String, messageId: String) =
        UriTemplate(CHANNEL_MESSAGE).expand(channelId, messageId)

}