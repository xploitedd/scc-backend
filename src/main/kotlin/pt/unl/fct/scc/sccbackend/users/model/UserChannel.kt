package pt.unl.fct.scc.sccbackend.users.model

import kotlinx.serialization.Serializable

@Serializable
data class UserChannel(
    val channel: String,
    val user: String
)
