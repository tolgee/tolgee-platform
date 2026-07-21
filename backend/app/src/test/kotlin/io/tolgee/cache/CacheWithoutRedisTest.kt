package io.tolgee.cache

import io.tolgee.component.cache.CacheFingerprintRegistry
import io.tolgee.constants.Caches
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.test.annotation.DirtiesContext
import java.lang.reflect.Modifier

@ContextRecreatingTest
@SpringBootTest(
  properties = [
    "tolgee.cache.enabled=true",
    "tolgee.internal.fake-mt-providers=false",
  ],
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CacheWithoutRedisTest : AbstractCacheTest() {
  @Autowired
  lateinit var cacheFingerprintRegistry: CacheFingerprintRegistry

  @Test
  fun `it has proper cache manager`() {
    assertThat(unwrappedCacheManager).isInstanceOf(CaffeineCacheManager::class.java)
  }

  @Test
  fun `every cache constant is shape-fingerprinted`() {
    val cacheNames =
      Caches::class.java.fields
        .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
        .map { it.get(null) as String }

    assertThat(cacheNames.filter { it.contains(CacheFingerprintRegistry.SEPARATOR) })
      .describedAs(
        "a logical cache name must not contain the physical-name separator '%s'",
        CacheFingerprintRegistry.SEPARATOR,
      ).isEmpty()

    val unfingerprinted = cacheNames.filter { cacheFingerprintRegistry.physicalName(it) == it }

    assertThat(unfingerprinted)
      .describedAs(
        "these caches are not shape-fingerprinted; declare each value type via a " +
          "DirectAccessCacheTypeProvider or a @Cacheable return type",
      ).isEmpty()
  }
}
