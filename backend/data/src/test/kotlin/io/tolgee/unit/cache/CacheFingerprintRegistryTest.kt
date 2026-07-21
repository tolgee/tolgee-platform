package io.tolgee.unit.cache

import io.tolgee.component.cache.CacheFingerprintRegistry
import io.tolgee.component.cache.CacheValueFingerprint
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationContext

class CacheFingerprintRegistryTest {
  private data class ShapeA(
    val a: Int,
  )

  private data class ShapeB(
    val a: Int,
    val b: Long,
  )

  private val applicationContext =
    mock<ApplicationContext>().apply {
      whenever(beanDefinitionNames).thenReturn(emptyArray())
    }

  private val registry = CacheFingerprintRegistry(applicationContext, CacheValueFingerprint())

  @Test
  fun `passes an unknown cache name through unchanged`() {
    registry.physicalName("unknownCache").assert.isEqualTo("unknownCache")
  }

  @Test
  fun `is idempotent on already-physical names`() {
    val physical = registry.physicalName("rateLimits", ShapeA::class)
    registry.physicalName(physical).assert.isEqualTo(physical)
  }

  @Test
  fun `routes different shapes of the same cache to different physical names`() {
    registry
      .physicalName("rateLimits", ShapeA::class)
      .assert
      .isNotEqualTo(registry.physicalName("rateLimits", ShapeB::class))
  }

  @Test
  fun `resolves a shape to a stable physical name`() {
    registry
      .physicalName("rateLimits", ShapeA::class)
      .assert
      .isEqualTo(registry.physicalName("rateLimits", ShapeA::class))
  }
}
