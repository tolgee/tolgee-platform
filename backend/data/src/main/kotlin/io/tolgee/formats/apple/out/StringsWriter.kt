package io.tolgee.formats.apple.out

import io.tolgee.formats.MobileStringEscaper

class StringsWriter {
  private val content = StringBuilder()

  fun addEntry(
    key: String,
    value: String,
    comment: String? = null,
  ) {
    comment?.let {
      val escaped = escapeComment(it)
      content.append("/* $escaped */\n")
    }
    content.append("\"${key.escaped()}\" = \"${value.escaped()}\";\n\n")
  }

  private fun escapeComment(s: String): String {
    return s.replace("*/", "*\\/")
  }

  private fun String.escaped(): String {
    return MobileStringEscaper(
      string = this,
      escapeApos = false,
      keepPercentSignEscaped = true,
      quoteMoreWhitespaces = false,
      escapeNewLines = true,
      escapeQuotes = true,
      utfSymbolCharacter = 'U',
    ).escape()
  }

  val result: String
    get() = content.toString()
}
