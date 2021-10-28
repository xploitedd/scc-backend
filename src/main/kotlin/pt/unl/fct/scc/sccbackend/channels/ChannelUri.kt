package pt.unl.fct.scc.sccbackend.channels

import org.springframework.web.util.UriTemplate

object ChannelUri {

    const val CHANNELS = "/channels"
    const val CHANNEL = "$CHANNELS/{channelId}"

    fun forChannel(channelId: String) = UriTemplate(CHANNEL).expand(channelId)

}