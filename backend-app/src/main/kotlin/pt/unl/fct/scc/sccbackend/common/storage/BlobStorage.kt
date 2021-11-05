package pt.unl.fct.scc.sccbackend.common.storage

interface BlobStorage {

    suspend fun upload(blobName: String, info: BlobInfo)

    suspend fun download(blobName: String): BlobInfo

    suspend fun exists(blobName: String): Boolean

    suspend fun delete(blobName: String)

}