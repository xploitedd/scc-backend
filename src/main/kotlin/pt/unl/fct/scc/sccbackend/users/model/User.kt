package pt.unl.fct.scc.sccbackend.users.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.springframework.security.crypto.password.PasswordEncoder

@Serializable
data class UserInput(
    val nickname: String,
    val name: String,
    val password: String,
    val photo: String
)

@Serializable
data class User(
    @SerialName("_id") val nickname: String,
    val name: String,
    val password: String,
    val photo: String
)

fun UserInput.hashPassword(encoder: PasswordEncoder) = User(
    nickname,
    name,
    encoder.encode(password),
    photo
)