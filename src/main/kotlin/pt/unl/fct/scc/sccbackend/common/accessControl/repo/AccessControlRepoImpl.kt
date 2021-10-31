package pt.unl.fct.scc.sccbackend.common.accessControl.repo

import org.litote.kmongo.eq
import org.springframework.stereotype.Repository
import pt.unl.fct.scc.sccbackend.common.cache.RedisClientProvider
import pt.unl.fct.scc.sccbackend.common.cache.getV
import pt.unl.fct.scc.sccbackend.common.database.KMongoTM
import pt.unl.fct.scc.sccbackend.users.model.User

@Repository
class AccessControlRepoImpl(
    val tm: KMongoTM,
    val redis: RedisClientProvider
) : AccessControlRepo {

    override suspend fun getUserByUsername(username: String): User? {
        val user = redis.use { getV<User>("user:$username") }
        if (user != null)
            return user

        return tm.use { db ->
            val col = db.getCollection<User>()
            col.findOne(User::nickname eq username)
        }
    }

}