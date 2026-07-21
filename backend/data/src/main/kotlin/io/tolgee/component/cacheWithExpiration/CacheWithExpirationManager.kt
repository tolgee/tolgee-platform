package io.tolgee.component.cacheWithExpiration

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.cache.CacheFingerprintRegistry
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class CacheWithExpirationManager(
  private val cacheManager: CacheManager,
  private val currentDateProvider: CurrentDateProvider,
  private val cacheFingerprintRegistry: CacheFingerprintRegistry,
) {
  fun getCache(
    name: String,
    valueType: KClass<*>,
  ): CacheWithExpiration? {
    return cacheManager
      .getCache(cacheFingerprintRegistry.physicalName(name, valueType))
      ?.let { CacheWithExpiration(it, currentDateProvider) }
  }
}
