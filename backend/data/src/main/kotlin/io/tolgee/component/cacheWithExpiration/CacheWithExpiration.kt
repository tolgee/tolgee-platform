package io.tolgee.component.cacheWithExpiration

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.ResilientCacheAccessor
import org.springframework.cache.Cache
import org.springframework.cache.Cache.ValueWrapper
import java.time.Duration

class CacheWithExpiration(
  private val cache: Cache,
  private val currentDateProvider: CurrentDateProvider,
  private val resilientCacheAccessor: ResilientCacheAccessor,
) {
  fun <T : Any?> get(key: Any): T? =
    resilientCacheAccessor.get(cache, key, CachedWithExpiration::class.java)?.let { getNotExpiredValue(key, it) }

  fun getWrapper(key: Any): ValueWrapper? {
    val cached = resilientCacheAccessor.get(cache, key, CachedWithExpiration::class.java) ?: return null
    val value = getNotExpiredValue<Any>(key, cached)
    return ValueWrapper { value }
  }

  private fun <T> getNotExpiredValue(
    key: Any,
    it: CachedWithExpiration,
  ): T? {
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
    return null
  }

  fun put(
    key: Any,
    value: Any?,
    expiration: Duration,
  ) {
    this.cache.put(key, CachedWithExpiration(currentDateProvider.date.time + expiration.toMillis(), value))
  }

  fun evict(key: Any) {
    this.cache.evict(key)
  }

  fun clear() {
    this.cache.clear()
  }
}
