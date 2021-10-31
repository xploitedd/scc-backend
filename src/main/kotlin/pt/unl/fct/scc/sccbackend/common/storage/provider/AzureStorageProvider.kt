package pt.unl.fct.scc.sccbackend.common.storage.provider

import com.azure.storage.blob.BlobContainerAsyncClient
import com.azure.storage.blob.BlobContainerClientBuilder
import org.springframework.stereotype.Service
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

private const val ENV_STORAGE_CONNECTION_STRING = "STORAGE_CONNECTION_STRING"
private const val ENV_STORAGE_CONTAINER = "STORAGE_CONTAINER"

@Service
class AzureStorageProvider : ReadOnlyProperty<Any?, BlobContainerAsyncClient> {

    override operator fun getValue(thisRef: Any?, property: KProperty<*>): BlobContainerAsyncClient =
        BlobContainerClientBuilder().let {
            val connectionString = System.getenv(ENV_STORAGE_CONNECTION_STRING)
                ?: throw Exception("Environment variable $ENV_STORAGE_CONNECTION_STRING is not defined")

            val containerName = System.getenv(ENV_STORAGE_CONTAINER)
                ?: throw Exception("Environment variable $ENV_STORAGE_CONTAINER is not defined")

            it.connectionString(connectionString)
                .containerName(containerName)
                .buildAsyncClient()
        }

}