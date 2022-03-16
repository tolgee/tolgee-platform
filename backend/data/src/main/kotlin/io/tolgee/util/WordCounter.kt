package io.tolgee.util

import com.ibm.icu.text.BreakIterator
import com.ibm.icu.util.ULocale

object WordCounter {
  fun countWords(text: String, languageTag: String): Int {
    val uLocale = getLocaleFromTag(languageTag)
    val iterator: BreakIterator = BreakIterator.getWordInstance(uLocale)
    iterator.setText(text)

    val words: MutableList<String> = ArrayList()
    var start: Int = iterator.first()
    var end: Int = iterator.next()
    while (end != BreakIterator.DONE) {
      val word = text.substring(start, end)
      if (!word.matches("[\\p{P} \\t\\n\\r]".toRegex())) {
        words.add(word)
      }
      start = end
      end = iterator.next()
    }
    return words.size
  }

  fun getLocaleFromTag(tag: String): ULocale {
    var result = ULocale.forLanguageTag(tag)
    if (result.language.isNotBlank()) {
      return result
    }

    val languagePart = tag.replace("^([A-Za-z]+).*".toRegex(), "$1")
    result = ULocale.forLanguageTag(languagePart)
    if (result.language.isNotBlank()) {
      return result
    }

    return ULocale.ENGLISH
  }
}
