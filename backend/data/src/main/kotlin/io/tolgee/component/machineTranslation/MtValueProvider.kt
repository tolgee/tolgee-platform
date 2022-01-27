package io.tolgee.component.machineTranslation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

interface MtValueProvider {
  val isEnabled: Boolean

  /**
   * Translates the text using the service
   */
  fun translate(text: String, sourceLanguageTag: String, targetLanguageTag: String): String?

  /**
   * Calculates credit price of the provider
   */
  fun calculatePrice(text: String): Int

  fun translate(texts: List<String>, sourceLanguageTag: String, targetLanguageTag: String): List<String>?

  fun translate(text: String, sourceLanguageTag: String, targetLanguageTags: List<String>): List<String?> {
    return runBlocking(Dispatchers.IO) {
      targetLanguageTags.map { targetLanguageTag ->
        async {
          translate(text, sourceLanguageTag, targetLanguageTag)
        }
      }.awaitAll()
    }
  }
}
