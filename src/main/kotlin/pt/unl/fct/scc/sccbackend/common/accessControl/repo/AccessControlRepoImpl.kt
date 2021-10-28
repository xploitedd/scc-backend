package pt.unl.fct.scc.sccbackend.common.accessControl.repo

import org.litote.kmongo.eq
import org.springframework.stereotype.Repository
import pt.unl.fct.scc.sccbackend.common.database.KMongoTM
import pt.unl.fct.scc.sccbackend.users.model.User

@Repository
class AccessControlRepoImpl(val tm: KMongoTM) : AccessControlRepo {

    override suspend fun getUserByUsername(username: String) = tm.use { db ->
        val col = db.getCollection<User>()
        col.findOne(User::nickname eq username)
    }

}