package io.tolgee.unit.cache

import io.tolgee.component.cache.CacheSchemaRegistry
import io.tolgee.constants.Caches
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.Test

class CacheSchemaRegistryTest {
  private val registry = CacheSchemaRegistry()

  @Test
  fun `getSchemaRevisions returns all tracked caches`() {
    val revisions = registry.getSchemaRevisions()

    assertThat(revisions).containsKey(Caches.RATE_LIMITS)
    assertThat(revisions[Caches.RATE_LIMITS]).isEqualTo(1)
  }

  @Test
  fun `getCachesToClear returns caches with newer revisions`() {
    val storedRevisions = mapOf(Caches.RATE_LIMITS to 0)

    val cachesToClear = registry.getCachesToClear(storedRevisions)

    assertThat(cachesToClear).contains(Caches.RATE_LIMITS)
  }

  @Test
  fun `getCachesToClear returns empty when revisions match`() {
    val storedRevisions = mapOf(Caches.RATE_LIMITS to 1)

    val cachesToClear = registry.getCachesToClear(storedRevisions)

    assertThat(cachesToClear).isEmpty()
  }

  @Test
  fun `getCachesToClear returns empty when stored revision is higher`() {
    val storedRevisions = mapOf(Caches.RATE_LIMITS to 999)

    val cachesToClear = registry.getCachesToClear(storedRevisions)

    assertThat(cachesToClear).isEmpty()
  }

  @Test
  fun `getCachesToClear treats missing stored revision as zero`() {
    val storedRevisions = emptyMap<String, Int>()

    val cachesToClear = registry.getCachesToClear(storedRevisions)

    // All tracked caches should be cleared since stored = 0 for all
    assertThat(cachesToClear).contains(Caches.RATE_LIMITS)
  }

  @Test
  fun `getCachesToClear ignores unknown stored revisions`() {
    val storedRevisions =
      mapOf(
        Caches.RATE_LIMITS to 1,
        "unknown-cache" to 5,
      )

    val cachesToClear = registry.getCachesToClear(storedRevisions)

    // Unknown cache should not cause any issues
    assertThat(cachesToClear).isEmpty()
  }
}
