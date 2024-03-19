package io.tolgee.util

import com.ibm.icu.text.BreakIterator
import io.tolgee.formats.getULocaleFromTag

object WordCounter {
  private val NON_WORD = """[\p{P} \t\n\r~!@#$%^&*()_+{}\[\]:;,.<>/?-]""".toRegex()
  private val LANGUAGE_PART = "^([A-Za-z]+).*".toRegex()

  fun countWords(
    text: String,
    languageTag: String,
  ): Int {
    val uLocale = getULocaleFromTag(languageTag)
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
}
