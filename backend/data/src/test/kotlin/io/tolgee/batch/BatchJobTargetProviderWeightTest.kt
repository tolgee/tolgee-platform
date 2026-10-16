package io.tolgee.batch

import io.tolgee.batch.data.TranslationTargetProduct
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class BatchJobTargetProviderWeightTest {
  private val provider = BatchJobTargetProvider(mock(), mock())

  @Test
  fun `a product weighs its ids, not its items`() {
    val product = TranslationTargetProduct((1L..1_000_000L).toList(), listOf(1L, 2L, 3L))

    provider.estimatedBytes(product).assert.isEqualTo(1_000_003 * 24)
  }

  @Test
  fun `decoded map items weigh more than numbers`() {
    provider.estimatedBytes(listOf(mapOf("keyId" to 1L), mapOf("keyId" to 2L))).assert.isEqualTo(2 * 256)
    provider.estimatedBytes(listOf(1L, 2L)).assert.isEqualTo(2 * 24)
  }

  @Test
  fun `a huge target does not overflow the weight`() {
    val hugeTarget =
      object : AbstractList<Any>() {
        override val size = 10_000_000

        override fun get(index: Int): Any = mapOf("keyId" to index)
      }

    provider.estimatedBytes(hugeTarget).assert.isEqualTo(Int.MAX_VALUE)
  }
}
