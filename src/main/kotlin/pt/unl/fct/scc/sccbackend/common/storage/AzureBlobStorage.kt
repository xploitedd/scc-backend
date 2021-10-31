package pt.unl.fct.scc.sccbackend.common.storage

import com.azure.core.util.BinaryData
import com.azure.storage.blob.BlobContainerAsyncClient
import com.azure.storage.blob.models.BlobHttpHeaders
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import pt.unl.fct.scc.sccbackend.common.NotFoundException

@Service
class AzureBlobStorage(val containerClient: BlobContainerAsyncClient) : BlobStorage {

    override suspend fun upload(blobName: String, info: BlobInfo) {
        val blob = containerClient.getBlobAsyncClient(blobName)

        blob.upload(BinaryData.fromBytes(info.data))
            .awaitSingle()

        val httpHeaders = BlobHttpHeaders()
            .setContentType(info.contentType)

        blob.setHttpHeaders(httpHeaders)
            .awaitSingleOrNull()
    }

    override suspend fun download(blobName: String): BlobInfo {
        val blob = containerClient.getBlobAsyncClient(blobName)
        // TODO: change these exceptions
        if (!blob.exists().awaitSingle())
            throw NotFoundException()

        val content = blob.downloadContent()
            .awaitSingle()
            .toBytes()

        val contentType = blob.properties
            .awaitSingle()
            .contentType

        return BlobInfo(content, contentType)
    }

    override suspend fun exists(blobName: String): Boolean {
        val blob = containerClient.getBlobAsyncClient(blobName)
        return blob.exists().awaitSingle()
    }

    override suspend fun delete(blobName: String) {
        val blob = containerClient.getBlobAsyncClient(blobName)
        if (!blob.exists().awaitSingle())
            throw NotFoundException()

        blob.delete()
            .awaitSingleOrNull()
    }

}