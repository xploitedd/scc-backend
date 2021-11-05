package pt.unl.fct.scc.sccbackend.users.model

import kotlinx.serialization.Serializable
import org.litote.kmongo.newId

@Serializable
data class UserChannel(
    val channel: String,
    val user: String,
    val _id: String = newId<UserChannel>().toString()
) {
    override fun equals(other: Any?): Boolean {
        if (other == null)
            return false

        if (other !is UserChannel)
            return false

        return channel == other.channel && user == other.user
    }

    override fun hashCode(): Int {
        var result = channel.hashCode()
        result = 31 * result + user.hashCode()
        return result
    }
}