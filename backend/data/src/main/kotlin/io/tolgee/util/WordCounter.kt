package io.tolgee.util

import com.ibm.icu.text.BreakIterator
import io.tolgee.formats.getULocaleFromTag

object WordCounter {
  private val NON_WORD = """[\p{P} \t\n\r~!@#$%^&*()_+{}\[\]:;,.<>/?-]""".toRegex()
  private val LANGUAGE_PART = "^([A-Za-z]+).*".toRegex()

  // BreakIterator construction is expensive (~ms per call) and the result is not
  // thread-safe. Keep one instance per thread per language tag — bulk imports
  // call countWords millions of times with a small set of locales.
  private val iteratorCache: ThreadLocal<MutableMap<String, BreakIterator>> =
    ThreadLocal.withInitial { mutableMapOf() }

  fun countWords(
    text: String,
    languageTag: String,
  ): Int {
    val iterator =
      iteratorCache.get().getOrPut(languageTag) {
        BreakIterator.getWordInstance(getULocaleFromTag(languageTag))
      }
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
