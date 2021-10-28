package pt.unl.fct.scc.sccbackend.channels

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pt.unl.fct.scc.sccbackend.channels.model.*
import pt.unl.fct.scc.sccbackend.channels.repo.ChannelRepository
import pt.unl.fct.scc.sccbackend.common.BadRequestException
import pt.unl.fct.scc.sccbackend.common.ForbiddenException
import pt.unl.fct.scc.sccbackend.common.NotFoundException
import pt.unl.fct.scc.sccbackend.users.model.PublicUserReducedDto
import pt.unl.fct.scc.sccbackend.users.model.User
import pt.unl.fct.scc.sccbackend.users.model.toPublicReducedDto

@RestController
class ChannelsController(val repo: ChannelRepository) {

    @GetMapping(ChannelUri.CHANNELS)
    suspend fun getChannels(
        user: User?
    ): ResponseEntity<List<ChannelReducedDto>> {
        val channels = repo.getChannels(user)
            .map { it.toReducedDto() }

        return ResponseEntity.ok(channels)
    }

    @PostMapping(ChannelUri.CHANNELS)
    suspend fun createChannel(
        user: User,
        @RequestBody channelInput: ChannelCreateInput
    ): ResponseEntity<Any> {
        val channel = repo.createChannel(channelInput.toChannel(user))
        repo.subscribeToChannel(channel, user)

        return ResponseEntity.created(ChannelUri.forChannel(channel.channelId))
            .build()
    }

    @GetMapping(ChannelUri.CHANNEL)
    suspend fun getChannel(
        user: User?,
        @PathVariable channelId: String
    ): ResponseEntity<ChannelReducedDto> {
        val channel = repo.getChannel(channelId)
        checkChannelReadAccess(user, channel)

        return ResponseEntity.ok(channel.toReducedDto())
    }

    @PatchMapping(ChannelUri.CHANNEL)
    suspend fun updateChannel(
        user: User,
        @PathVariable channelId: String,
        @RequestBody channelInput: ChannelUpdateInput
    ): ResponseEntity<Any> {
        val channel = repo.getChannel(channelId)
        checkChannelWriteAccess(user, channel)

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
        checkChannelWriteAccess(user, channel)

        repo.deleteChannel(channel)
        return ResponseEntity.noContent().build()
    }

    @GetMapping(ChannelUri.CHANNEL_MEMBERS)
    suspend fun getChannelMembers(
        user: User?,
        @PathVariable channelId: String
    ): ResponseEntity<List<PublicUserReducedDto>> {
        val channel = repo.getChannel(channelId)
        checkChannelReadAccess(user, channel)

        val members = repo.getChannelMembers(channel)
            .map { it.toPublicReducedDto() }

        return ResponseEntity.ok(members)
    }

    @PutMapping(ChannelUri.CHANNEL_MEMBERS)
    suspend fun addChannelMember(
        user: User,
        @PathVariable channelId: String,
        @RequestBody memberInput: ChannelNewMemberInput?
    ): ResponseEntity<Any> {
        val channel = repo.getChannel(channelId)
        if (channel.private || memberInput != null) {
            checkChannelWriteAccess(user, channel)
            repo.addChannelMember(
                channel,
                memberInput ?: throw BadRequestException("The user id parameter should be specified")
            )
        } else {
            repo.subscribeToChannel(channel, user)
        }

        return ResponseEntity.noContent().build()
    }

    private suspend fun checkChannelReadAccess(user: User?, channel: Channel) {
        if (channel.private) {
            if (user == null)
                throw NotFoundException()

            if (channel.owner != user.userId && !repo.isUserInChannel(channel, user))
                throw NotFoundException()
        }
    }

    private suspend fun checkChannelWriteAccess(user: User, channel: Channel) {
        if (channel.owner != user.userId) {
            // users that do not have knowledge of the channel should always get a 404
            if (repo.isUserInChannel(channel, user))
                throw ForbiddenException()
            else
                throw NotFoundException()
        }
    }

}