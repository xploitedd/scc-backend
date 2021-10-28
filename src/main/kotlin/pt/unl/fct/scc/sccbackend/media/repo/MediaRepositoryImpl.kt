package pt.unl.fct.scc.sccbackend.media.repo

import org.litote.kmongo.eq
import org.springframework.stereotype.Repository
import pt.unl.fct.scc.sccbackend.common.UnauthorizedException
import pt.unl.fct.scc.sccbackend.common.database.KMongoTM
import pt.unl.fct.scc.sccbackend.media.model.Media

@Repository
abstract class MediaRepositoryImpl(val tm: KMongoTM) : MediaRepository {


    /** **/
    override suspend fun uploadMedia(media: Media) = tm.useTransaction { db ->
        val col = db.getCollection<Media>()
        col.insertOne(media)
        media
    }

    /** **/
    override suspend fun downloadMedia(blobName: String) = tm.use { db ->
        val col = db.getCollection<Media>()
        val data = col.findOne(Media::blobName eq blobName)
            ?: throw UnauthorizedException()

        (data.content).toByteArray()
    }

    /** **/
    override suspend fun deleteMedia(blobName: String) = tm.use { db ->
        val col = db.getCollection<Media>()
        col.deleteOne(Media::blobName eq blobName)

        Unit
    }

}
