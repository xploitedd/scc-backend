package pt.unl.fct.scc.sccbackend.media.repo

import org.litote.kmongo.eq
import org.springframework.stereotype.Repository
import pt.unl.fct.scc.sccbackend.common.NotFoundException
import pt.unl.fct.scc.sccbackend.common.cache.RedisClientProvider
import pt.unl.fct.scc.sccbackend.common.cache.getV
import pt.unl.fct.scc.sccbackend.common.cache.setV
import pt.unl.fct.scc.sccbackend.common.database.KMongoTM
import pt.unl.fct.scc.sccbackend.common.storage.BlobInfo
import pt.unl.fct.scc.sccbackend.common.storage.BlobStorage
import pt.unl.fct.scc.sccbackend.media.model.Media

@Repository
class MediaRepositoryImpl(
    val tm: KMongoTM,
    val bs: BlobStorage,
    val redis: RedisClientProvider
) : MediaRepository {

    override suspend fun createMedia(blobInfo: BlobInfo) = tm.use { db ->
        val col = db.getCollection<Media>()
        if (!bs.exists(blobInfo.hash))
            bs.upload(blobInfo.hash, blobInfo)

        val media = Media(blobInfo.contentType, blobInfo.hash)
        col.insertOne(media)

        redis.run { setV("media:${media.mediaId}", media) }

        media
    }

    override suspend fun getMedia(mediaId: String): BlobInfo {
        val media = redis.fetch { getV<Media>("media:${mediaId}")?.hash }
            ?: tm.use { db ->
                val col = db.getCollection<Media>()
                val media = col.findOne(Media::mediaId eq mediaId)
                    ?: throw NotFoundException()

                redis.run { setV("media:${mediaId}", media) }

                media.hash
            }

        return bs.download(media)
    }

}
