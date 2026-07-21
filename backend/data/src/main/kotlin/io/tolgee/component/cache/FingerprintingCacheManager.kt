package io.tolgee.component.cache

import org.springframework.cache.Cache
import org.springframework.cache.CacheManager

/**
 * Rewrites cache names to their shape-fingerprinted physical name before delegating, so a change to a
 * cached type's shape transparently switches to a fresh physical cache. Names that carry no known
 * fingerprint (or are already physical) pass through unchanged.
 */
class FingerprintingCacheManager(
  private val delegate: CacheManager,
  private val registry: CacheFingerprintRegistry,
) : CacheManager {
  override fun getCache(name: String): Cache? = delegate.getCache(registry.physicalName(name))

  override fun getCacheNames(): Collection<String> = delegate.cacheNames
}
