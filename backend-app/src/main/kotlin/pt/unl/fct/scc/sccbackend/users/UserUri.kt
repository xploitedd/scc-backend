package pt.unl.fct.scc.sccbackend.users

import org.springframework.web.util.UriTemplate

object UserUri {

    const val USERS = "/users"
    const val USER = "$USERS/{username}"
    const val USER_ME = "$USERS/me"
    const val USER_CHANNELS = "$USER_ME/channels"

    fun forUser(username: String) = UriTemplate(USER).expand(username)

}