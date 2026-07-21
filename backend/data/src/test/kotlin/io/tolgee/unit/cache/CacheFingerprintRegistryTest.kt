package io.tolgee.unit.cache

import io.tolgee.component.cache.CacheFingerprintRegistry
import io.tolgee.component.cache.CacheValueFingerprint
import io.tolgee.component.cache.DirectAccessCacheTypeProvider
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationContext
import kotlin.reflect.KClass

class CacheFingerprintRegistryTest {
  data class ShapeA(
    val a: Int,
  )

  data class ShapeB(
    val a: Int,
    val b: Long,
  )

  class ShapeACacheableBean {
    @Cacheable("annotatedCache")
    fun get(): ShapeA = ShapeA(1)
  }

  class ShapeBCacheableBean {
    @Cacheable("annotatedCache")
    fun get(): ShapeB = ShapeB(1, 2)
  }

  class MultiTypeCacheableBean {
    @Cacheable("annotatedCache")
    fun a(): ShapeA = ShapeA(1)

    @Cacheable("annotatedCache")
    fun b(): ShapeB = ShapeB(1, 2)
  }

  private fun registry(
    beans: Map<String, Class<*>> = emptyMap(),
    directTypes: Map<String, KClass<*>> = emptyMap(),
  ): CacheFingerprintRegistry {
    val ctx = mock<ApplicationContext>()
    whenever(ctx.beanDefinitionNames).thenReturn(beans.keys.toTypedArray())
    beans.forEach { (name, type) -> whenever(ctx.getType(name)).thenReturn(type) }
    val providers =
      if (directTypes.isEmpty()) {
        emptyList()
      } else {
        listOf(DirectAccessCacheTypeProvider { directTypes })
      }
    return CacheFingerprintRegistry(ctx, CacheValueFingerprint(), providers)
  }

  @Test
  fun `passes an unknown cache name through unchanged`() {
    registry().physicalName("unknownCache").assert.isEqualTo("unknownCache")
  }

  @Test
  fun `is idempotent on already-physical names`() {
    val reg = registry(directTypes = mapOf("rateLimits" to ShapeA::class))
    val physical = reg.physicalName("rateLimits")
    physical.assert.contains(CacheFingerprintRegistry.SEPARATOR)
    reg.physicalName(physical).assert.isEqualTo(physical)
  }

  @Test
  fun `fingerprints a directly-declared cache`() {
    registry(directTypes = mapOf("rateLimits" to ShapeA::class))
      .physicalName("rateLimits")
      .assert
      .startsWith("rateLimits${CacheFingerprintRegistry.SEPARATOR}")
  }

  @Test
  fun `fingerprints an annotation-discovered cache`() {
    registry(beans = mapOf("shapeABean" to ShapeACacheableBean::class.java))
      .physicalName("annotatedCache")
      .assert
      .startsWith("annotatedCache${CacheFingerprintRegistry.SEPARATOR}")
  }

  @Test
  fun `a shape change to an annotated cache's return type changes its physical name`() {
    val withShapeA = registry(beans = mapOf("bean" to ShapeACacheableBean::class.java)).physicalName("annotatedCache")
    val withShapeB = registry(beans = mapOf("bean" to ShapeBCacheableBean::class.java)).physicalName("annotatedCache")
    withShapeA.assert.isNotEqualTo(withShapeB)
  }

  @Test
  fun `aggregates multiple return types targeting the same cache`() {
    registry(beans = mapOf("bean" to MultiTypeCacheableBean::class.java))
      .physicalName("annotatedCache")
      .assert
      .startsWith("annotatedCache${CacheFingerprintRegistry.SEPARATOR}")
  }
}
