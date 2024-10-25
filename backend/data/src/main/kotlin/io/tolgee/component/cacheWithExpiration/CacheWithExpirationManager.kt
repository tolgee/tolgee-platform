package io.tolgee.component.cacheWithExpiration

import io.tolgee.component.CurrentDateProvider
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CacheWithExpirationManager(
  private val cacheManager: CacheManager,
  private val currentDateProvider: CurrentDateProvider,
) {
  fun getCache(name: String): CacheWithExpiration? =
    cacheManager.getCache(name)?.let { CacheWithExpiration(it, currentDateProvider) }

  fun putCache(
    cacheName: String,
    userId: Long,
    isUserStillValid: Boolean,
    expiration: Duration = Duration.ofMinutes(10),
  ) {
    val cache = getCache(cacheName)
    cache?.put(userId, isUserStillValid, expiration)
  }
}
