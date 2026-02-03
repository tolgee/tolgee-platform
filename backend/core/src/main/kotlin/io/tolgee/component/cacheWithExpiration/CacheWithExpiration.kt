package io.tolgee.component.cacheWithExpiration

import io.tolgee.component.CurrentDateProvider
import org.springframework.cache.Cache
import org.springframework.cache.Cache.ValueWrapper
import java.time.Duration

class CacheWithExpiration(
  private val cache: Cache,
  private val currentDateProvider: CurrentDateProvider,
) {
  fun <T : Any?> get(key: Any): T? {
    this.cache.get(key, CachedWithExpiration::class.java)?.let {
      return getNotExpiredValue(key, it)
    }
    return null
  }

  fun getWrapper(key: Any): ValueWrapper? {
    this.cache.get(key)?.let { valueWrapper ->
      val it = valueWrapper.get() as CachedWithExpiration? ?: return ValueWrapper { null }
      val value = getNotExpiredValue<Any>(key, it)
      return ValueWrapper { value }
    }
    return null
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
