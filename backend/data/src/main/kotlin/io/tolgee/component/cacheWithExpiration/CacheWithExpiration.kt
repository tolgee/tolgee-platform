package io.tolgee.component.cacheWithExpiration

import io.tolgee.component.CurrentDateProvider
import org.springframework.cache.Cache
import java.time.Duration

class CacheWithExpiration(
  private val cache: Cache,
  private val currentDateProvider: CurrentDateProvider
) {
  fun <T : Any?> get(key: Any, type: Class<T>?): T? {
    this.cache.get(key, CachedWithExpiration::class.java)?.let {
      if (it.expiresAt > currentDateProvider.date.time) {
        try {
          @Suppress("UNCHECKED_CAST")
          return it.data as? T
        } catch (e: ClassCastException) {
          this.cache.evict(key)
          return null
        }
      }
      this.cache.evict(key)
    }
    return null
  }

  fun put(key: Any, value: Any?, expiration: Duration) {
    this.cache.put(key, CachedWithExpiration(currentDateProvider.date.time + expiration.toMillis(), value))
  }
}
