package pt.unl.fct.scc.sccbackend.users.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.litote.kmongo.newId
import org.springframework.security.crypto.password.PasswordEncoder

@Serializable
data class UserCreateInput(
    val nickname: String,
    val name: String,
    val password: String,
    val photo: String
)

@Serializable
data class UserUpdateInput(
    val name: String? = null,
    val password: String? = null,
    val photo: String? = null
)

@Serializable
data class User(
    val nickname: String,
    val name: String,
    val password: String,
    val photo: String,
    @SerialName("_id")
    val userId: String = newId<User>().toString()
)

@Serializable
data class PublicUserReducedDto(
    val nickname: String,
    val name: String,
    val photo: String
)

fun User.toPublicReducedDto() = PublicUserReducedDto(
    nickname,
    name,
    photo
)

fun UserCreateInput.toUser(encoder: PasswordEncoder) = User(
    nickname,
    name,
    encoder.encode(password),
    photo
)

fun UserUpdateInput.toUser(user: User, encoder: PasswordEncoder) = User(
    user.nickname,
    name ?: user.name,
    password?.let {
        // check whether the password has changed
        if (!encoder.matches(it, user.password))
            encoder.encode(it)
        else
            user.password
    } ?: user.password,
    photo ?: user.photo,
    user.userId
)