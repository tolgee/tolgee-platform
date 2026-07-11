package io.tolgee.component.contentDelivery.cachePurging.azureFrontDoor

import com.azure.identity.ClientSecretCredential
import com.azure.identity.ClientSecretCredentialBuilder
import io.tolgee.model.contentDelivery.AzureFrontDoorConfig

class AzureCredentialProvider {
  fun get(config: AzureFrontDoorConfig): ClientSecretCredential {
    return ClientSecretCredentialBuilder()
      .clientId(config.clientId)
      .clientSecret(config.clientSecret)
      .tenantId(config.tenantId)
      .build()
  }
}
