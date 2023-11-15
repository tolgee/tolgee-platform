package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.cdn.cache-purging")
class CdnCachePurgingProperties {
  var azureFrontDoor: CdnAzureFrontDoorProperties = CdnAzureFrontDoorProperties()
}
