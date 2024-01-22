package io.tolgee.util

import com.ibm.icu.text.BreakIterator
import com.ibm.icu.util.ULocale

object WordCounter {
  private val NON_WORD = """[\p{P} \t\n\r~!@#$%^&*()_+{}\[\]:;,.<>/?-]""".toRegex()
  private val LANGUAGE_PART = "^([A-Za-z]+).*".toRegex()

  fun countWords(
    text: String,
    languageTag: String,
  ): Int {
    val uLocale = getLocaleFromTag(languageTag)
    val iterator: BreakIterator = BreakIterator.getWordInstance(uLocale)
    iterator.setText(text)

    var words = 0
    var start: Int = iterator.first()
    var end: Int = iterator.next()
    while (end != BreakIterator.DONE) {
      val word = text.substring(start, end)
      if (!NON_WORD.matches(word)) {
        words = words.inc()
      }
      start = end
      end = iterator.next()
    }
    return words
  }

  fun getLocaleFromTag(tag: String): ULocale {
    var result = ULocale.forLanguageTag(tag)
    if (result.language.isNotBlank()) {
      return result
    }

    val languagePart = tag.replace(LANGUAGE_PART, "$1")
    result = ULocale.forLanguageTag(languagePart)
    if (result.language.isNotBlank()) {
      return result
    }

    return ULocale.ENGLISH
  }
}
