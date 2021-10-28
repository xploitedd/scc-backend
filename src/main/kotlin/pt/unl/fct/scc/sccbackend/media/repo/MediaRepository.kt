package pt.unl.fct.scc.sccbackend.media.repo


import pt.unl.fct.scc.sccbackend.media.model.Media

interface MediaRepository {

    suspend fun uploadMedia(media: Media) : Media

    suspend fun downloadMedia(blobName: String) : ByteArray

    suspend fun deleteMedia(blobName: String)

}