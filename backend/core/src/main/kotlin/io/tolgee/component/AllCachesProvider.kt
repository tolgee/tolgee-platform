package io.tolgee.component

import io.tolgee.constants.Caches
import org.springframework.stereotype.Component

@Component
class AllCachesProvider(
  private val additionalCacheProviders: List<AdditionalCachesProvider>,
) {
  fun getAllCaches(): List<String> {
    return Caches.caches + additionalCacheProviders.flatMap { it.getAdditionalCaches() }
  }
}
