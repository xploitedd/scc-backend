package pt.unl.fct.scc.sccbackend.common.storage.provider

import com.azure.storage.blob.BlobContainerAsyncClient
import com.azure.storage.blob.BlobContainerClientBuilder
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service

private const val ENV_STORAGE_CONNECTION_STRING = "STORAGE_CONNECTION_STRING"
private const val ENV_STORAGE_CONTAINER = "STORAGE_CONTAINER"

@Service
class AzureStorageProvider {

    @Bean
    fun getContainerClient(): BlobContainerAsyncClient {
        val connectionString = System.getenv(ENV_STORAGE_CONNECTION_STRING)
            ?: throw Exception("Environment variable $ENV_STORAGE_CONNECTION_STRING is not defined")

        val containerName = System.getenv(ENV_STORAGE_CONTAINER)
            ?: throw Exception("Environment variable $ENV_STORAGE_CONTAINER is not defined")

        return BlobContainerClientBuilder()
            .connectionString(connectionString)
            .containerName(containerName)
            .buildAsyncClient()
    }

}