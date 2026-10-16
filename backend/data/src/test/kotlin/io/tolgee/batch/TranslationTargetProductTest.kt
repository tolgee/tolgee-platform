package io.tolgee.batch

import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.data.TranslationTargetProduct
import io.tolgee.batch.data.TranslationTargetProductOrder
import io.tolgee.batch.data.TranslationTargetProductSpec
import io.tolgee.model.batch.BatchJob
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

class TranslationTargetProductTest {
  private val keyIds = listOf(10L, 11L, 12L, 13L, 14L)
  private val languageIds = listOf(1L, 2L, 3L)

  private val keyMajorList =
    keyIds.flatMap { keyId -> languageIds.map { BatchTranslationTargetItem(keyId, it) } }

  private val languageMajorList =
    languageIds.flatMap { languageId -> keyIds.map { BatchTranslationTargetItem(it, languageId) } }

  @Test
  fun `key major product equals the materialized key by key list`() {
    val product = TranslationTargetProduct(keyIds, languageIds)
    product.size.assert.isEqualTo(15)
    product.toList().assert.isEqualTo(keyMajorList)
    product.assert.isEqualTo(keyMajorList)
  }

  @Test
  fun `language major product equals the materialized language by language list`() {
    val product = TranslationTargetProduct(keyIds, languageIds, TranslationTargetProductOrder.LANGUAGE_MAJOR)
    product.toList().assert.isEqualTo(languageMajorList)
  }

  @Test
  fun `chunks of a product are the same as chunks of the materialized list`() {
    val product = TranslationTargetProduct(keyIds, languageIds)
    listOf(1, 2, 4, 5, 7, 15, 100).forEach { chunkSize ->
      val expected = keyMajorList.chunked(chunkSize)
      BatchJob.chunkCount(chunkSize, product.size).assert.isEqualTo(expected.size)
      expected.forEachIndexed { chunkNumber, expectedChunk ->
        BatchJob.getChunk(chunkSize, product, chunkNumber).assert.isEqualTo(expectedChunk)
      }
    }
  }

  @Test
  fun `chunk size 0 means a single chunk with the whole target`() {
    val product = TranslationTargetProduct(keyIds, languageIds)
    BatchJob.chunkCount(0, product.size).assert.isEqualTo(1)
    BatchJob.getChunk(0, product, 0).assert.isEqualTo(keyMajorList)
  }

  @Test
  fun `empty product has no chunks`() {
    val product = TranslationTargetProduct(listOf(), languageIds)
    product.size.assert.isEqualTo(0)
    BatchJob.chunkCount(3, product.size).assert.isEqualTo(0)
  }

  @Test
  fun `out of range index and chunk throw`() {
    val product = TranslationTargetProduct(keyIds, languageIds)
    assertThatThrownBy { product[15] }.isInstanceOf(IndexOutOfBoundsException::class.java)
    assertThatThrownBy { BatchJob.getChunk(4, product, 4) }.isInstanceOf(IndexOutOfBoundsException::class.java)
  }

  @Test
  fun `spec survives json round trip`() {
    val mapper = jacksonObjectMapper()
    val spec = TranslationTargetProduct(keyIds, languageIds, TranslationTargetProductOrder.LANGUAGE_MAJOR).spec
    val parsed = mapper.readValue<TranslationTargetProductSpec>(mapper.writeValueAsString(spec))
    parsed.assert.isEqualTo(spec)
    parsed
      .toTarget()
      .toList()
      .assert
      .isEqualTo(languageMajorList)
  }

  @Test
  fun `product serializes like the materialized list`() {
    val mapper = jacksonObjectMapper()
    mapper
      .writeValueAsString(TranslationTargetProduct(keyIds, languageIds))
      .assert
      .isEqualTo(mapper.writeValueAsString(keyMajorList))
  }
}
