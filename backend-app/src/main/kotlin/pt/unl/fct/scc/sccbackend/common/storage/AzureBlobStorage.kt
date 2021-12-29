package pt.unl.fct.scc.sccbackend.common.storage

import com.azure.core.util.BinaryData
import com.azure.storage.blob.models.BlobHttpHeaders
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pt.unl.fct.scc.sccbackend.common.NotFoundException
import pt.unl.fct.scc.sccbackend.common.storage.provider.AzureStorageProvider
import java.security.MessageDigest
import java.util.*

class AzureBlobStorage(
    val storageProvider: AzureStorageProvider,
    val digester: MessageDigest
) : BlobStorage {

    companion object {
        private val log = LoggerFactory.getLogger(AzureBlobStorage::class.java)
    }

    override suspend fun upload(blobName: String, info: BlobInfo) {
        val blob = storageProvider.containerClient
            .getBlobAsyncClient(blobName)

        blob.upload(BinaryData.fromBytes(info.data))
            .awaitSingle()

        val httpHeaders = BlobHttpHeaders()
            .setContentType(info.contentType)

        blob.setHttpHeaders(httpHeaders)
            .awaitSingleOrNull()
    }

    override suspend fun download(blobName: String): BlobInfo {
        val blob = storageProvider.containerClient
            .getBlobAsyncClient(blobName)

        return if (!blob.exists().awaitSingle()) {
            val content = storageProvider.fetchFromReplicas(blobName)

            coroutineScope {
                launch {
                    log.info("Replicating $blobName to storage container")
                    blob.upload(BinaryData.fromBytes(content.data))
                        .awaitSingle()

                    val httpHeaders = BlobHttpHeaders()
                        .setContentType(content.contentType)

                    blob.setHttpHeaders(httpHeaders)
                        .awaitSingleOrNull()

                    log.info("The blob $blobName was replicated successfully")
                }
            }

            content
        } else {
            val content = blob.downloadContent()
                .awaitSingle()
                .toBytes()

            val contentType = blob.properties
                .awaitSingle()
                .contentType

            val hash = Base64.getUrlEncoder()
                .encodeToString(digester.digest(content))

            BlobInfo(content, contentType, hash)
        }
    }

    override suspend fun exists(blobName: String): Boolean {
        val blob = storageProvider.containerClient
            .getBlobAsyncClient(blobName)

        return blob.exists().awaitSingle()
    }

    override suspend fun delete(blobName: String) {
        val blob = storageProvider.containerClient
            .getBlobAsyncClient(blobName)

        if (!blob.exists().awaitSingle())
            throw NotFoundException()

        blob.delete()
            .awaitSingleOrNull()
    }

}