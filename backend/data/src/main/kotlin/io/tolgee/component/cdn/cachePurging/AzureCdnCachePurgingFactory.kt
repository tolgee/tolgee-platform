package io.tolgee.component.cdn.cachePurging

import io.tolgee.model.cdn.AzureFrontDoorConfig
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class AzureCdnCachePurgingFactory(
  private val restTemplate: RestTemplate
) : CdnCachePurgingFactory {
  override fun create(config: Any): AzureCdnCachePurging {
    return AzureCdnCachePurging(config as AzureFrontDoorConfig, restTemplate, AzureCredentialProvider())
  }
}
