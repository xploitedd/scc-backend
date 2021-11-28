package pt.unl.fct.scc.sccbackend.common.storage.provider

import com.azure.core.http.policy.HttpLogDetailLevel
import com.azure.core.management.AzureEnvironment
import com.azure.core.management.profile.AzureProfile
import com.azure.identity.AzureCliCredentialBuilder
import com.azure.identity.DefaultAzureCredentialBuilder
import com.azure.resourcemanager.AzureResourceManager
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service

private const val AZURE_CLI_AUTH_ENV = "AZURE_CLI_AUTH"

@Service
class AzureResourceManagerProvider {

    private val useCliAuth: Boolean = System.getenv(AZURE_CLI_AUTH_ENV)
        .toBoolean()

    @Bean
    fun getResourceManager(): AzureResourceManager {
        val profile = AzureProfile(AzureEnvironment.AZURE)
        val cred = if (useCliAuth) {
            AzureCliCredentialBuilder()
                .build()
        } else {
            DefaultAzureCredentialBuilder()
                .build()
        }

        return AzureResourceManager.configure()
            .withLogLevel(HttpLogDetailLevel.BASIC)
            .authenticate(cred, profile)
            .withDefaultSubscription()
    }

}