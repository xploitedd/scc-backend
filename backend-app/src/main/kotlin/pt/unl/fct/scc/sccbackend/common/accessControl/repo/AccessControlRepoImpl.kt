package pt.unl.fct.scc.sccbackend.common.accessControl.repo

import org.litote.kmongo.eq
import org.springframework.stereotype.Repository
import pt.unl.fct.scc.sccbackend.common.cache.RedisClientProvider
import pt.unl.fct.scc.sccbackend.common.cache.getV
import pt.unl.fct.scc.sccbackend.common.cache.setV
import pt.unl.fct.scc.sccbackend.common.database.KMongoTM
import pt.unl.fct.scc.sccbackend.users.model.User

@Repository
class AccessControlRepoImpl(
    val tm: KMongoTM,
    val redis: RedisClientProvider
) : AccessControlRepo {

    override suspend fun getUserByUsername(username: String): User? {
        val cached = redis.fetch { getV<User>("user:$username") }
        if (cached != null)
            return cached

        return tm.use { db ->
            val col = db.getCollection<User>()
            val user = col.findOne(User::nickname eq username)
            if (user != null)
                redis.run { setV("user:$username", user) }

            user
        }
    }

}