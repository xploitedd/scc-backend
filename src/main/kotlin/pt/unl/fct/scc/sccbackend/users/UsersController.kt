package pt.unl.fct.scc.sccbackend.users

import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import pt.unl.fct.scc.sccbackend.channels.model.ChannelReducedDto
import pt.unl.fct.scc.sccbackend.users.model.User
import pt.unl.fct.scc.sccbackend.users.model.UserInput
import pt.unl.fct.scc.sccbackend.users.model.hashPassword
import pt.unl.fct.scc.sccbackend.users.repo.UserRepository

@RestController
class UsersController(
    val repo: UserRepository,
    val passwordEncoder: PasswordEncoder
) {

    @PostMapping(UserUri.USERS)
    suspend fun createUser(
        @RequestBody user: UserInput
    ): ResponseEntity<Any> {
        val createdUser = repo.createUser(user.hashPassword(passwordEncoder))
        return ResponseEntity.created(UserUri.forUser(createdUser.nickname))
            .build()
    }

    @PatchMapping(UserUri.USER)
    suspend fun updateUser(
        @PathVariable("userId") userId: String
    ) {
        // TODO
    }

    @DeleteMapping(UserUri.USER)
    suspend fun deleteUser(
        user: User,
        @PathVariable("userId") userId: String
    ): ResponseEntity<Any> {
        // TODO: Exception Handler, with custom exceptions
        if (userId != user.nickname)
            throw Exception("The user does not have access to this resource")

        repo.deleteUser(user)
        return ResponseEntity.noContent()
            .build()
    }

    @GetMapping(UserUri.USER_CHANNELS)
    suspend fun getUserChannels(
        user: User,
        @PathVariable userId: String
    ): ResponseEntity<List<ChannelReducedDto>> {
        // TODO: Exception Handler, with custom exceptions
        if (userId != user.nickname)
            throw Exception("The user does not have access to this resource")

        val channels = repo.getUserChannels(user)
        return ResponseEntity.ok(channels)
    }

}