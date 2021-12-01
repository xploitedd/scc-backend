package pt.unl.fct.scc.sccbackend.channels

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import pt.unl.fct.scc.sccbackend.channels.model.*
import pt.unl.fct.scc.sccbackend.channels.repo.ChannelRepository
import pt.unl.fct.scc.sccbackend.common.BadRequestException
import pt.unl.fct.scc.sccbackend.common.ForbiddenException
import pt.unl.fct.scc.sccbackend.common.NotFoundException
import pt.unl.fct.scc.sccbackend.common.pagination.Pagination
import pt.unl.fct.scc.sccbackend.users.model.PublicUserReducedDto
import pt.unl.fct.scc.sccbackend.users.model.User
import pt.unl.fct.scc.sccbackend.users.model.toPublicReducedDto

@RestController
class ChannelsController(val repo: ChannelRepository) {

    @GetMapping(ChannelUri.CHANNELS)
    suspend fun getChannels(
        user: User?,
        pagination: Pagination
    ): ResponseEntity<List<ChannelReducedDto>> {
        val channels = repo.getChannels(user, pagination)
            .map { it.toReducedDto() }

        return ResponseEntity.ok(channels)
    }

    @GetMapping(ChannelUri.CHANNELS_TRENDING)
    suspend fun getTrendingChannels(
        user: User?,
        pagination: Pagination
    ): ResponseEntity<List<ChannelReducedDto>> {
        val trending = repo.getTrendingChannels(user, pagination)
            .map { it.toReducedDto() }

        return ResponseEntity.ok(trending)
    }

    @PostMapping(ChannelUri.CHANNELS)
    suspend fun createChannel(
        user: User,
        @RequestBody channelInput: ChannelCreateInput
    ): ResponseEntity<ChannelReducedDto> {
        val channel = repo.createChannel(channelInput.toChannel(user))
        repo.addChannelMember(channel, user.nickname)

        return ResponseEntity.created(ChannelUri.forChannel(channel.channelId))
            .body(channel.toReducedDto())
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
        checkChannelOwnerAccess(user, channel)

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
        checkChannelOwnerAccess(user, channel)

        repo.deleteChannel(channel)
        return ResponseEntity.noContent().build()
    }

    @GetMapping(ChannelUri.CHANNEL_MEMBERS)
    suspend fun getChannelMembers(
        user: User?,
        @PathVariable channelId: String,
        pagination: Pagination
    ): ResponseEntity<List<PublicUserReducedDto>> {
        val channel = repo.getChannel(channelId)
        checkChannelReadAccess(user, channel)

        val members = repo.getChannelMembers(channel, pagination)
            .map { it.toPublicReducedDto() }

        return ResponseEntity.ok(members)
    }

    @PutMapping(ChannelUri.CHANNEL_MEMBERS)
    suspend fun addChannelMember(
        user: User,
        @PathVariable channelId: String,
        @RequestBody memberInput: ChannelMemberInput?
    ): ResponseEntity<Any> {
        val channel = repo.getChannel(channelId)
        if (channel.private || memberInput != null) {
            checkChannelOwnerAccess(user, channel)
            val username = memberInput?.username
                ?: throw BadRequestException("The username parameter should be specified")

            repo.addChannelMember(channel, username)
        } else {
            repo.addChannelMember(channel, user.nickname)
        }

        return ResponseEntity.noContent().build()
    }

    @DeleteMapping(ChannelUri.CHANNEL_MEMBERS)
    suspend fun removeChannelMember(
        user: User,
        @PathVariable channelId: String,
        @RequestBody memberInput: ChannelMemberInput?
    ): ResponseEntity<Any> {
        val channel = repo.getChannel(channelId)
        if (memberInput != null) {
            if (channel.private)
                checkChannelOwnerAccess(user, channel)

            val username = memberInput.username
            repo.removeChannelMember(channel, username)
        } else {
            repo.removeChannelMember(channel, user.nickname)
        }

        return ResponseEntity.noContent().build()
    }

    @GetMapping(ChannelUri.CHANNEL_MESSAGES)
    suspend fun getChannelMessages(
        user: User?,
        pagination: Pagination,
        @PathVariable channelId: String
    ): ResponseEntity<List<ChannelMessageReducedDto>> {
        val channel = repo.getChannel(channelId)
        checkChannelReadAccess(user, channel)

        val messages = repo.getChannelMessages(channel, pagination)
            .map { it.toReducedDto() }

        return ResponseEntity.ok(messages)
    }

    @GetMapping(ChannelUri.CHANNEL_MESSAGES_SEARCH)
    suspend fun searchChannelMessages(
        user: User?,
        pagination: Pagination,
        @PathVariable channelId: String,
        @RequestParam query: String
    ): ResponseEntity<List<ChannelMessageReducedDto>> {
        val channel = repo.getChannel(channelId)
        checkChannelReadAccess(user, channel)

        val messages = repo.searchChannelMessages(channel, query, pagination)
            .map { it.toReducedDto() }

        return ResponseEntity.ok(messages)
    }

    @PostMapping(ChannelUri.CHANNEL_MESSAGES)
    suspend fun createChannelMessage(
        user: User,
        @PathVariable channelId: String,
        @RequestBody messageInput: ChannelMessageInput
    ): ResponseEntity<ChannelMessageReducedDto> {
        val channel = repo.getChannel(channelId)
        checkChannelWriteAccess(user, channel)

        val message = repo.createChannelMessage(channel, messageInput.toChannelMessage(channel, user))
        return ResponseEntity.created(ChannelUri.forChannelMessage(channelId, message.messageId))
            .body(message.toReducedDto())
    }

    @DeleteMapping(ChannelUri.CHANNEL_MESSAGE)
    suspend fun deleteChannelMessage(
        user: User,
        @PathVariable channelId: String,
        @PathVariable messageId: String
    ): ResponseEntity<Any> {
        val channel = repo.getChannel(channelId)
        checkChannelWriteAccess(user, channel)

        val message = repo.getChannelMessage(channel, messageId)
        if (user.nickname != message.user && user.nickname != channel.owner)
            throw ForbiddenException()

        repo.deleteChannelMessage(channel, message)
        return ResponseEntity.noContent().build()
    }

    private suspend fun checkChannelReadAccess(user: User?, channel: Channel) {
        if (channel.private) {
            if (user == null)
                throw NotFoundException()

            if (channel.owner != user.nickname && !repo.isUserInChannel(channel, user))
                throw NotFoundException()
        }
    }

    private suspend fun checkChannelWriteAccess(user: User, channel: Channel) {
        if (!repo.isUserInChannel(channel, user)) {
            if (channel.private)
                throw NotFoundException()
            else
                throw ForbiddenException()
        }
    }

    private suspend fun checkChannelOwnerAccess(user: User, channel: Channel) {
        if (channel.owner != user.nickname) {
            // users that do not have knowledge of the channel should always get a 404
            if (repo.isUserInChannel(channel, user))
                throw ForbiddenException()
            else
                throw NotFoundException()
        }
    }

}