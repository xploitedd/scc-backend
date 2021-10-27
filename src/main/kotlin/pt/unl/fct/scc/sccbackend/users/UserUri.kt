package pt.unl.fct.scc.sccbackend.users

import org.springframework.web.util.UriTemplate

object UserUri {

    const val USERS = "/users"
    const val USER = "$USERS/{username}"
    const val USER_CHANNELS = "$USER/channels"

    fun forUser(username: String) = UriTemplate(USER).expand(username)

}