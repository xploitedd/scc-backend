package pt.unl.fct.scc.sccbackend.users

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import pt.unl.fct.scc.sccbackend.channels.model.ChannelReducedDto
import pt.unl.fct.scc.sccbackend.common.ForbiddenException
import pt.unl.fct.scc.sccbackend.users.model.*
import pt.unl.fct.scc.sccbackend.users.repo.UserRepository

@RestController
class UsersController(
    val repo: UserRepository,
    val passwordEncoder: PasswordEncoder
) {

    @PostMapping(UserUri.USERS)
    suspend fun createUser(
        @RequestBody userInput: UserCreateInput
    ): ResponseEntity<Any> {
        val createdUser = repo.createUser(userInput.toUser(passwordEncoder))
        return ResponseEntity.created(UserUri.forUser(createdUser.nickname))
            .build()
    }

    @GetMapping(UserUri.USER)
    suspend fun getUser(
        @PathVariable username: String
    ): ResponseEntity<PublicUserReducedDto> {
        val user = repo.getUser(username)
        return ResponseEntity.ok(user.toPublicReducedDto())
    }

    @PatchMapping(UserUri.USER)
    suspend fun updateUser(
        user: User,
        @PathVariable username: String,
        @RequestBody userInput: UserUpdateInput
    ): ResponseEntity<Any> {
        if (username != user.nickname)
            throw ForbiddenException()

        val updatedUser = repo.updateUser(userInput.toUser(user, passwordEncoder))
        return ResponseEntity.noContent()
            .header(HttpHeaders.CONTENT_LOCATION, UserUri.forUser(updatedUser.nickname).toString())
            .build()
    }

    @DeleteMapping(UserUri.USER)
    suspend fun deleteUser(
        user: User,
        @PathVariable username: String
    ): ResponseEntity<Any> {
        if (username != user.nickname)
            throw ForbiddenException()

        repo.deleteUser(user)
        return ResponseEntity.noContent()
            .build()
    }

    @GetMapping(UserUri.USER_CHANNELS)
    suspend fun getUserChannels(
        user: User,
        @PathVariable username: String
    ): ResponseEntity<List<ChannelReducedDto>> {
        if (username != user.nickname)
            throw ForbiddenException()

        val channels = repo.getUserChannels(user)
        return ResponseEntity.ok(channels)
    }

}