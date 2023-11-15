package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.cdn")
class CdnProperties {
  var publicUrlPrefix: String? = null
  var storage: CdnStorageProperties = CdnStorageProperties()
  var cachePurging: CdnCachePurgingProperties = CdnCachePurgingProperties()
}
