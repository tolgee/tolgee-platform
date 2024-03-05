package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.cache")
@DocProperty(
  description =
    "At the expense of higher memory footprint, " +
      "Tolgee can use a cache to reduce the stress on the database and fetch the\n" +
      "data it needs faster. Cache is also used to track certain states, such as rate limits.",
  displayName = "Cache",
)
class CacheProperties(
  @DocProperty(description = "Whether Tolgee should use a cache.")
  var enabled: Boolean = false,
  @DocProperty(
    description =
      "Whether Tolgee should use Redis to store cache data instead of storing it in-memory.\n" +
        ":::info\n" +
        "In a distributed environment, you **should** use a Redis server " +
        "to ensure consistent enforcement of rate limits, as\n" +
        "they heavily rely on cache. For a simple single-node deployment, in-memory cache is sufficient.\n" +
        ":::\n" +
        "\n" +
        ":::info\n" +
        "Tolgee uses [Redisson](https://github.com/redisson/redisson) to interface with the Redis server. " +
        "You can find the properties Redisson expects [here]" +
        "(https://github.com/redisson/redisson/tree/56ea7f5/redisson-spring-boot-starter" +
        "#2-add-settings-into-applicationsettings-file).\n" +
        ":::\n\n",
  )
  var useRedis: Boolean = false,
  @DocProperty(description = "TTL of cache data, in milliseconds.", defaultExplanation = "≈ 2 hours")
  var defaultTtl: Long = 120 * 60 * 1000,
  @DocProperty(
    description =
      "Maximum size of the Caffeine cache. " +
        "When exceeded, some entries will be purged from cache. Set to -1 to disable size limitation.\n" +
        "This has no effect when Redis cache is used. See\n" +
        "[Caffeine's documentation about size-based eviction]" +
        "(https://github.com/ben-manes/caffeine/wiki/Eviction#size-based)",
  )
  var caffeineMaxSize: Long = -1,
  @DocProperty(
    description = "Whether to clean the cache on Tolgee startup",
  )
  var cleanOnStartup: Boolean = true,
)
