package pt.unl.fct.scc.sccbackend.channels

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pt.unl.fct.scc.sccbackend.channels.model.*
import pt.unl.fct.scc.sccbackend.channels.repo.ChannelRepository
import pt.unl.fct.scc.sccbackend.common.ForbiddenException
import pt.unl.fct.scc.sccbackend.common.NotFoundException
import pt.unl.fct.scc.sccbackend.users.model.User

@RestController
class ChannelsController(val repo: ChannelRepository) {

    @PostMapping(ChannelUri.CHANNELS)
    suspend fun createChannel(
        user: User,
        @RequestBody channelInput: ChannelCreateInput
    ): ResponseEntity<Any> {
        val channel = repo.createChannel(channelInput.toChannel(user))
        return ResponseEntity.created(ChannelUri.forChannel(channel.channelId))
            .build()
    }

    @GetMapping(ChannelUri.CHANNEL)
    suspend fun getChannel(
        user: User?,
        @PathVariable channelId: String
    ): ResponseEntity<ChannelReducedDto> {
        val channel = repo.getChannel(channelId)
        if (channel.private) {
            if (user == null)
                throw NotFoundException()

            if (channel.owner != user.userId && !repo.isUserInChannel(user, channel))
                throw NotFoundException()
        }

        return ResponseEntity.ok(channel.toReducedDto())
    }

    @PatchMapping(ChannelUri.CHANNEL)
    suspend fun updateChannel(
        user: User,
        @PathVariable channelId: String,
        @RequestBody channelInput: ChannelUpdateInput
    ): ResponseEntity<Any> {
        val channel = repo.getChannel(channelId)
        checkIfChannelOwner(user, channel)

        repo.updateChannel(channelInput.toChannel(channel))
        return ResponseEntity.noContent()
            .header(HttpHeaders.CONTENT_LOCATION, ChannelUri.forChannel(channelId).toString())
            .build()
    }

    @DeleteMapping(ChannelUri.CHANNEL)
    suspend fun deleteChannel(
        user: User,
        @PathVariable channelId: String
    ): ResponseEntity<Any> {
        val channel = repo.getChannel(channelId)
        checkIfChannelOwner(user, channel)

        repo.deleteChannel(channel)
        return ResponseEntity.noContent().build()
    }

    private suspend fun checkIfChannelOwner(user: User, channel: Channel) {
        if (channel.owner != user.userId) {
            // users that do not have knowledge of the channel should always get a 404
            if (repo.isUserInChannel(user, channel))
                throw ForbiddenException()
            else
                throw NotFoundException()
        }
    }

}