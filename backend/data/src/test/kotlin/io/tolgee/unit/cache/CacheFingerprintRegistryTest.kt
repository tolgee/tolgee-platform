package io.tolgee.unit.cache

import io.tolgee.component.cache.CacheFingerprintRegistry
import io.tolgee.component.cache.CacheValueFingerprint
import io.tolgee.component.cache.DirectAccessCacheTypeProvider
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
    return CacheFingerprintRegistry(ctx, CacheValueFingerprint(), providers).apply { afterSingletonsInstantiated() }
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

  @Test
  fun `the annotation type wins when a cache is both declared and annotated`() {
    val reg =
      registry(
        beans = mapOf("bean" to ShapeBCacheableBean::class.java),
        directTypes = mapOf("annotatedCache" to ShapeA::class),
      )
    reg
      .physicalName("annotatedCache")
      .assert
      .isEqualTo("annotatedCache${CacheFingerprintRegistry.SEPARATOR}${CacheValueFingerprint().compute(ShapeB::class)}")
  }

  @Test
  fun `a bean whose type cannot be resolved does not abort the scan`() {
    val ctx = mock<ApplicationContext>()
    whenever(ctx.beanDefinitionNames).thenReturn(arrayOf("badBean", "goodBean"))
    whenever(ctx.getType("badBean")).thenThrow(RuntimeException("boom"))
    whenever(ctx.getType("goodBean")).thenReturn(ShapeACacheableBean::class.java)

    CacheFingerprintRegistry(ctx, CacheValueFingerprint(), emptyList())
      .apply { afterSingletonsInstantiated() }
      .physicalName("annotatedCache")
      .assert
      .startsWith("annotatedCache${CacheFingerprintRegistry.SEPARATOR}")
  }

  @Test
  fun `a type that fails to fingerprint does not abort the build`() {
    val ctx = mock<ApplicationContext>()
    whenever(ctx.beanDefinitionNames).thenReturn(emptyArray())
    val fingerprint = mock<CacheValueFingerprint>()
    whenever(fingerprint.compute(ShapeA::class)).thenThrow(RuntimeException("boom"))
    whenever(fingerprint.compute(ShapeB::class)).thenReturn("goodfp")
    val provider = DirectAccessCacheTypeProvider { mapOf("badCache" to ShapeA::class, "goodCache" to ShapeB::class) }

    val reg = CacheFingerprintRegistry(ctx, fingerprint, listOf(provider)).apply { afterSingletonsInstantiated() }

    reg.physicalName("goodCache").assert.isEqualTo("goodCache${CacheFingerprintRegistry.SEPARATOR}goodfp")
    reg.physicalName("badCache").assert.isEqualTo("badCache")
  }

  @Test
  fun `physicalName fails loud when queried before the registry is built`() {
    val reg = CacheFingerprintRegistry(mock<ApplicationContext>(), CacheValueFingerprint(), emptyList())
    assertThrows<IllegalStateException> { reg.physicalName("anyCache") }
  }
}
