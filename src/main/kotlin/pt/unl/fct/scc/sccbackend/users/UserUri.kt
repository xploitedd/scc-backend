package pt.unl.fct.scc.sccbackend.users

import org.springframework.web.util.UriTemplate

object UserUri {

    const val USERS = "/users"
    const val USER = "$USERS/{userId}"
    const val USER_CHANNELS = "$USER/channels"

    fun forUser(userId: String) = UriTemplate(USER).expand(userId)

}