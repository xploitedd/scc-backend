package pt.unl.fct.scc.sccbackend.common.accessControl.repo

import pt.unl.fct.scc.sccbackend.users.model.User

interface AccessControlRepo {

    suspend fun getUserByUsername(username: String): User?

}