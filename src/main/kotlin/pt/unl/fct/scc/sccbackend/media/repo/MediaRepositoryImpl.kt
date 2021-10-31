package pt.unl.fct.scc.sccbackend.media.repo

import org.litote.kmongo.eq
import org.springframework.stereotype.Repository
import pt.unl.fct.scc.sccbackend.common.NotFoundException
import pt.unl.fct.scc.sccbackend.common.database.KMongoTM
import pt.unl.fct.scc.sccbackend.common.storage.BlobInfo
import pt.unl.fct.scc.sccbackend.common.storage.BlobStorage
import pt.unl.fct.scc.sccbackend.media.model.Media

@Repository
class MediaRepositoryImpl(
    val tm: KMongoTM,
    val bs: BlobStorage
) : MediaRepository {

    override suspend fun createMedia(blobInfo: BlobInfo) = tm.use { db ->
        val col = db.getCollection<Media>()
        val newMedia = Media(blobInfo.contentType)

        bs.upload(newMedia.blobName, blobInfo)
        col.insertOne(newMedia)
        newMedia
    }

    override suspend fun getMedia(blobName: String): BlobInfo = tm.use { db ->
        val col = db.getCollection<Media>()
        val media = col.findOne(Media::blobName eq blobName)
            ?: throw NotFoundException()

        bs.download(media.blobName)
    }

}
