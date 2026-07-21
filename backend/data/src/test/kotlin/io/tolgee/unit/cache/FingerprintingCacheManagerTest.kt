package io.tolgee.unit.cache

import io.tolgee.component.cache.CacheFingerprintRegistry
import io.tolgee.component.cache.FingerprintingCacheManager
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager

class FingerprintingCacheManagerTest {
  private val delegate = mock<CacheManager>()
  private val registry = mock<CacheFingerprintRegistry>()
  private val manager = FingerprintingCacheManager(delegate, registry)

  @Test
  fun `resolves the physical name through the registry before delegating`() {
    val physicalCache = mock<Cache>()
    whenever(registry.physicalName("userAccounts")).thenReturn("userAccounts--abc123")
    whenever(delegate.getCache("userAccounts--abc123")).thenReturn(physicalCache)

    manager.getCache("userAccounts").assert.isSameAs(physicalCache)
    verify(delegate).getCache("userAccounts--abc123")
  }

  @Test
  fun `delegates cache names untouched`() {
    whenever(delegate.cacheNames).thenReturn(listOf("a--1", "b--2"))
    manager.cacheNames.assert.containsExactly("a--1", "b--2")
  }
}
