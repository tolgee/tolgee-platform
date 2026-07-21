package io.tolgee.component.cache

import org.springframework.cache.Cache
import org.springframework.cache.CacheManager

/** Delegating [CacheManager] that funnels every cache name through [CacheFingerprintRegistry.physicalName]. */
class FingerprintingCacheManager(
  private val delegate: CacheManager,
  private val registry: CacheFingerprintRegistry,
) : CacheManager {
  override fun getCache(name: String): Cache? = delegate.getCache(registry.physicalName(name))

  override fun getCacheNames(): Collection<String> = delegate.cacheNames
}
