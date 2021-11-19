package io.tolgee.configuration.tolgee

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.cache")
class CacheProperties(
  var useRedis: Boolean = false,
  var defaultTtl: Long = 120 * 60 * 1000,
  var enabled: Boolean = false,
  var caffeineMaxSize: Long = -1,
)
