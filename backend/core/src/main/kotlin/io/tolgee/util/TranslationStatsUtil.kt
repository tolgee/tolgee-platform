package io.tolgee.util

object TranslationStatsUtil {
  fun getWordCount(
    text: String?,
    languageTag: String,
  ): Int {
    return text?.let { WordCounter.countWords(it, languageTag) } ?: 0
  }

  fun getCharacterCount(text: String?): Int {
    return text?.length ?: 0
  }
}
