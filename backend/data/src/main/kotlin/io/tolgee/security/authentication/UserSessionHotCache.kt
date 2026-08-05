package io.tolgee.security.authentication

import com.github.benmanes.caffeine.cache.Caffeine
import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.AuthenticationProperties
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Revocation state of sessions, kept out of the Spring cache abstraction on purpose: caching is
 * disabled by default, and this cache is what keeps the authentication path from hitting the
 * database on every request.
 */
@Component
class UserSessionHotCache(
  authenticationProperties: AuthenticationProperties,
  currentDateProvider: CurrentDateProvider,
) {
  class Entry(
    val revoked: Boolean,
    @Volatile var lastUsedWrittenAt: Long,
  )

  private val cache =
    Caffeine
      .newBuilder()
      .maximumSize(authenticationProperties.sessionAudit.sessionCacheMaxSize)
      .expireAfterWrite(Duration.ofMillis(authenticationProperties.sessionAudit.sessionCacheTtlMs))
      .ticker { TimeUnit.MILLISECONDS.toNanos(currentDateProvider.date.time) }
      .build<String, Entry>()

  fun get(deviceId: String): Entry? = cache.getIfPresent(deviceId)

  fun put(
    deviceId: String,
    entry: Entry,
  ) {
    cache.put(deviceId, entry)
  }

  fun evict(deviceId: String) {
    cache.invalidate(deviceId)
  }

  fun invalidateAll() {
    cache.invalidateAll()
  }
}
