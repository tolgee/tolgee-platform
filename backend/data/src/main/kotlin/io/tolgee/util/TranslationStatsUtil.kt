package io.tolgee.util

object TranslationStatsUtil {
  fun getWordCount(
    text: String?,
    languageTag: String,
  ): Int = text?.let { WordCounter.countWords(it, languageTag) } ?: 0

  fun getCharacterCount(text: String?): Int = text?.length ?: 0
}
