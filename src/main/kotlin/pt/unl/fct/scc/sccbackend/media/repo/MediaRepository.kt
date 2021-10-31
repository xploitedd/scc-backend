package pt.unl.fct.scc.sccbackend.media.repo

import pt.unl.fct.scc.sccbackend.common.storage.BlobInfo
import pt.unl.fct.scc.sccbackend.media.model.Media

interface MediaRepository {

    suspend fun createMedia(blobInfo: BlobInfo): Media

    suspend fun getMedia(blobName: String): BlobInfo

}