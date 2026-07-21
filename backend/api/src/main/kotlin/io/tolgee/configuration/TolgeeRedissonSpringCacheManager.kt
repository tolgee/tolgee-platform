package io.tolgee.configuration

import org.redisson.api.RedissonClient
import org.redisson.spring.cache.CacheConfig
import org.redisson.spring.cache.RedissonSpringCacheManager
import java.util.concurrent.ConcurrentHashMap

/**
 * Applies the default TTL to every cache, including ones created on demand. Redisson calls
 * [createDefaultConfig] for any cache name that has no explicit config, so this removes the need to
 * enumerate cache names up front — a cache that was never registered would otherwise be created with
 * no expiry at all.
 */
class TolgeeRedissonSpringCacheManager(
  redissonClient: RedissonClient,
  private val defaultTtl: Long,
) : RedissonSpringCacheManager(redissonClient, ConcurrentHashMap<String, CacheConfig>()) {
  override fun createDefaultConfig(): CacheConfig = CacheConfig(defaultTtl, defaultTtl)
}
