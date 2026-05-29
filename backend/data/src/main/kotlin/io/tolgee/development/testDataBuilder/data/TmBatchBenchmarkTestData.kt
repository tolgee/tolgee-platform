package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Language
import io.tolgee.model.key.Key

/**
 * Test fixture for benchmarking TM batch vs per-item lookups.
 *
 * Generates:
 * - [sourceKeyCount] "source" keys, each with English (base) text "common-text-{i % distinctSourceTexts}"
 *   plus a target translation in [germanLanguage]. These act as TM sources.
 * - [requestKeyCount] "request" keys, each with English text "common-text-{i % distinctSourceTexts}"
 *   but NO German translation — these are the chunk items we want pre-translated by TM.
 *
 * With distinctSourceTexts < sourceKeyCount, each request key will find a TM match
 * (a different source key with the same English text).
 */
class TmBatchBenchmarkTestData(
  private val sourceKeyCount: Int = 200,
  private val requestKeyCount: Int = 100,
  private val distinctSourceTexts: Int = 50,
) : BaseTestData() {
  lateinit var germanLanguage: Language
  val sourceKeys = mutableListOf<Key>()
  val requestKeys = mutableListOf<Key>()

  init {
    projectBuilder.apply {
      addLanguage {
        name = "German"
        tag = "de"
        germanLanguage = this
      }

      repeat(sourceKeyCount) { i ->
        val baseText = "common-text-${i % distinctSourceTexts}"
        addKey {
          name = "source-key-$i"
        }.build buildKey@{
          sourceKeys += this@buildKey.self
          addTranslation {
            key = this@buildKey.self
            language = englishLanguage
            text = baseText
          }
          addTranslation {
            key = this@buildKey.self
            language = germanLanguage
            text = "german-for-$baseText-from-source-$i"
          }
        }
      }

      repeat(requestKeyCount) { i ->
        val baseText = "common-text-${i % distinctSourceTexts}"
        addKey {
          name = "request-key-$i"
        }.build buildKey@{
          requestKeys += this@buildKey.self
          addTranslation {
            key = this@buildKey.self
            language = englishLanguage
            text = baseText
          }
        }
      }
    }
  }
}
