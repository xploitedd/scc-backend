package pt.unl.fct.scc.sccbackend.users.repo

import pt.unl.fct.scc.sccbackend.channels.model.ChannelReducedDto
import pt.unl.fct.scc.sccbackend.users.model.User

interface UserRepository {

    suspend fun createUser(user: User): User

    suspend fun getUser(username: String): User

    suspend fun updateUser(update: User): User

    suspend fun deleteUser(user: User)

    suspend fun getUserChannels(user: User): List<ChannelReducedDto>

}