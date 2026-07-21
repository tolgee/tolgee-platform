package io.tolgee.unit.cache

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.ResilientCacheAccessor
import io.tolgee.component.cacheWithExpiration.CacheWithExpiration
import io.tolgee.component.cacheWithExpiration.CachedWithExpiration
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.cache.Cache
import java.util.Date

class CacheWithExpirationTest {
  private val cache = mock<Cache>()
  private val currentDateProvider =
    mock<CurrentDateProvider>().apply {
      whenever(date).thenReturn(Date(1_000))
    }
  private val resilientCacheAccessor = mock<ResilientCacheAccessor>()
  private val cacheWithExpiration = CacheWithExpiration(cache, currentDateProvider, resilientCacheAccessor)

  @Test
  fun `getWrapper returns a present wrapper holding null for a cached-null entry`() {
    whenever(resilientCacheAccessor.get(eq(cache), any(), eq(CachedWithExpiration::class.java)))
      .thenReturn(CachedWithExpiration(expiresAt = 9_999, data = null))

    val wrapper = cacheWithExpiration.getWrapper("k")

    wrapper.assert.isNotNull
    wrapper!!.get().assert.isNull()
  }

  @Test
  fun `getWrapper returns null for a missing entry`() {
    whenever(resilientCacheAccessor.get(eq(cache), any(), eq(CachedWithExpiration::class.java))).thenReturn(null)

    cacheWithExpiration.getWrapper("k").assert.isNull()
  }
}
