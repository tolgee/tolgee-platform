package io.tolgee.component.cdn.cachePurging

import com.azure.identity.ClientSecretCredential
import com.azure.identity.ClientSecretCredentialBuilder
import io.tolgee.model.cdn.AzureFrontDoorConfig

class AzureCredentialProvider {
  fun get(config: AzureFrontDoorConfig): ClientSecretCredential {
    return ClientSecretCredentialBuilder()
      .clientId(config.clientId)
      .clientSecret(config.clientSecret)
      .tenantId(config.tenantId)
      .build()
  }
}
