package io.tolgee.service.translation

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.TmBatchLookupTestData
import io.tolgee.service.translationMemory.TmAutoTranslateProviderOssImpl
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

/**
 * Unit tests for the batched TM exact-match lookup. The OSS implementation lives on
 * [TmAutoTranslateProviderOssImpl]; the suggestion-panel interface
 * [TranslationMemoryService] is a separate concern (trigram similarity, plan gating).
 *
 * We autowire the OSS impl directly so EE bean overrides (if running with EE on the classpath)
 * don't change what this test asserts about the OSS lookup semantics.
 */
@SpringBootTest
@Transactional
class TranslationMemoryServiceBatchTest : AbstractSpringTest() {
  @Autowired
  private lateinit var ossProvider: TmAutoTranslateProviderOssImpl

  private lateinit var testData: TmBatchLookupTestData

  @BeforeEach
  fun setUp() {
    testData = TmBatchLookupTestData()
    testDataService.saveTestData(testData.root)
  }

  @Test
  fun `returns empty map for empty input`() {
    val result = ossProvider.getAutoTranslatedValuesForChunk(emptyList(), emptyMap(), emptyMap())
    assertThat(result).isEmpty()
  }

  @Test
  fun `returns TM match for single-item chunk`() {
    val items = listOf(testData.requestedKey.id to testData.germanLanguage.id)

    val result = ossProvider.getAutoTranslatedValuesForChunk(items, emptyMap(), emptyMap())

    val match = result[items.single()]
    assertThat(match).isNotNull
    assertThat(match!!.targetTranslationText).isIn("Hallo", "Hallo (alternative)")
    assertThat(match.baseTranslationText).isEqualTo("Hello")
    assertThat(match.keyId).isIn(testData.sourceKey.id, testData.duplicateSourceKey.id)
  }

  @Test
  fun `excludes self when requested key is also a source candidate`() {
    val items = listOf(testData.selfMatchKey.id to testData.germanLanguage.id)

    val result = ossProvider.getAutoTranslatedValuesForChunk(items, emptyMap(), emptyMap())

    val match = result[items.single()]
    assertThat(match).isNull()
  }

  @Test
  fun `returns no match when base text has no other source`() {
    val items = listOf(testData.uniqueKey.id to testData.germanLanguage.id)

    val result = ossProvider.getAutoTranslatedValuesForChunk(items, emptyMap(), emptyMap())

    assertThat(result[items.single()]).isNull()
  }

  @Test
  fun `handles multiple chunk items sharing the same base text and target language`() {
    val pair1 = testData.requestedKey.id to testData.germanLanguage.id
    val pair2 = testData.requestedKeySameText.id to testData.germanLanguage.id
    val items = listOf(pair1, pair2)

    val result = ossProvider.getAutoTranslatedValuesForChunk(items, emptyMap(), emptyMap())

    assertThat(result[pair1]).isNotNull
    assertThat(result[pair2]).isNotNull
    assertThat(result[pair1]!!.targetTranslationText).isIn("Hallo", "Hallo (alternative)")
    assertThat(result[pair2]!!.targetTranslationText).isIn("Hallo", "Hallo (alternative)")
    // Neither should select self
    assertThat(result[pair1]!!.keyId).isNotEqualTo(testData.requestedKey.id)
    assertThat(result[pair2]!!.keyId).isNotEqualTo(testData.requestedKeySameText.id)
  }

  @Test
  fun `handles mixed target languages in one chunk`() {
    val germanPair = testData.requestedKey.id to testData.germanLanguage.id
    val spanishPair = testData.requestedKey.id to testData.spanishLanguage.id
    val items = listOf(germanPair, spanishPair)

    val result = ossProvider.getAutoTranslatedValuesForChunk(items, emptyMap(), emptyMap())

    assertThat(result[germanPair]?.targetTranslationText).isIn("Hallo", "Hallo (alternative)")
    assertThat(result[spanishPair]?.targetTranslationText).isEqualTo("Hola")
  }

  @Test
  fun `ignores unrequested pairs from the cross-product result`() {
    // Ask only for (requestedKey, German). The query filters by keyIds IN (..) and
    // targetLangIds IN (..), but with a single key and single lang there is no cross-product.
    val items = listOf(testData.requestedKey.id to testData.germanLanguage.id)
    val result = ossProvider.getAutoTranslatedValuesForChunk(items, emptyMap(), emptyMap())
    assertThat(result.keys).containsExactly(items.single())
  }

  @Test
  fun `does not return entries for unrequested key+lang combinations`() {
    // requestedKey + German is asked. spanishLanguage is also a target lang for some other
    // chunk item conceptually, but for THIS call we only ask about (requestedKey, German).
    val items = listOf(testData.requestedKey.id to testData.germanLanguage.id)
    val result = ossProvider.getAutoTranslatedValuesForChunk(items, emptyMap(), emptyMap())
    val unrequestedPair = testData.requestedKey.id to testData.spanishLanguage.id
    assertThat(result).doesNotContainKey(unrequestedPair)
  }
}
