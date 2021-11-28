package pt.unl.fct.scc.sccbackend.common.storage.provider

import com.azure.resourcemanager.AzureResourceManager
import com.azure.storage.blob.BlobContainerAsyncClient
import com.azure.storage.blob.BlobContainerClientBuilder
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.stereotype.Service
import pt.unl.fct.scc.sccbackend.common.NotFoundException
import pt.unl.fct.scc.sccbackend.common.storage.BlobInfo
import reactor.core.publisher.Flux
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.*

private const val ENV_STORAGE_CONNECTION_STRING = "STORAGE_CONNECTION_STRING"
private const val ENV_STORAGE_CONTAINER = "STORAGE_CONTAINER"
private const val ENV_RESOURCE_PREFIX = "RESOURCE_PREFIX"
private const val STORAGE_CONNECTION_TEMPLATE = "DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s;EndpointSuffix=core.windows.net"

@Service
class AzureStorageProvider(
    val resourceManager: AzureResourceManager,
    val digester: MessageDigest
) {

    private val connectionString = System.getenv(ENV_STORAGE_CONNECTION_STRING)
        ?: throw Exception("Environment variable $ENV_STORAGE_CONNECTION_STRING is not defined")

    private val containerName = System.getenv(ENV_STORAGE_CONTAINER)
        ?: throw Exception("Environment variable $ENV_STORAGE_CONTAINER is not defined")

    private val resourcePrefix = System.getenv(ENV_RESOURCE_PREFIX)
        ?: throw Exception("Environment variable $ENV_RESOURCE_PREFIX is not defined")

    val containerClient: BlobContainerAsyncClient = BlobContainerClientBuilder()
        .connectionString(connectionString)
        .containerName(containerName)
        .buildAsyncClient()

    suspend fun fetchFromReplicas(blobName: String): BlobInfo {
        val blob = getStorageReplicas().map { it.getBlobAsyncClient(blobName) }
            .firstOrNull { it.exists().awaitSingle() }
            ?: throw NotFoundException()

        val content = blob.downloadContent()
            .awaitSingle()
            .toBytes()

        val contentType = blob.properties
            .awaitSingle()
            .contentType

        val hash = Base64.getUrlEncoder()
            .encodeToString(digester.digest(content))

        return BlobInfo(content, contentType, hash)
    }

    private fun getStorageReplicas(): Flow<BlobContainerAsyncClient> {
        return resourceManager.storageAccounts()
            .listAsync()
            .asFlow()
            .filter { it.name().contains(resourcePrefix) }
            .map {
                val key = it.keysAsync
                    .asFlow()
                    .firstOrNull()
                    ?.first()
                    ?.value()

                if (key == null) {
                    ""
                } else {
                    STORAGE_CONNECTION_TEMPLATE.format(it.name(), key)
                }
            }
            .filter { it.isNotEmpty() }
            .map {
                BlobContainerClientBuilder()
                    .connectionString(it)
                    .containerName(containerName)
                    .buildAsyncClient()
            }
    }

}