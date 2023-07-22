package io.tolgee.component.cacheWithExpiration

import io.tolgee.component.CurrentDateProvider
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

@Component
class CacheWithExpirationManager(
  private val cacheManager: CacheManager,
  private val currentDateProvider: CurrentDateProvider
) {
  fun getCache(name: String): CacheWithExpiration? {
    return cacheManager.getCache(name)?.let { CacheWithExpiration(it, currentDateProvider) }
  }
}
