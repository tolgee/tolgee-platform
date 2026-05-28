package io.tolgee.util

/**
 * Strips every codepoint outside the XML 1.0 `Char` production
 * (`#x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]`).
 *
 * We walk codepoints rather than UTF-16 chars so valid surrogate pairs (emoji etc.) survive
 * intact while lone surrogates, illegal C0 controls (everything but tab/LF/CR), and BMP
 * noncharacters get dropped. Apply this to any user-controlled string that lands in an XML
 * document — both DOM (`textContent`, attribute values) and StAX (`writeCharacters`) writers
 * reject illegal codepoints with hard exceptions (`TransformerException` and
 * `WstxIOException` respectively), so a single bad codepoint can sink the whole export.
 */
fun sanitizeXmlText(text: String): String {
  if (text.isEmpty()) return text
  val out = StringBuilder(text.length)
  var i = 0
  while (i < text.length) {
    val cp = text.codePointAt(i)
    if (isValidXmlChar(cp)) out.appendCodePoint(cp)
    i += Character.charCount(cp)
  }
  return out.toString()
}

private fun isValidXmlChar(cp: Int): Boolean {
  if (cp == 0x9 || cp == 0xA || cp == 0xD) return true
  if (cp in 0x20..0xD7FF) return true
  if (cp in 0xE000..0xFFFD) return true
  if (cp in 0x10000..0x10FFFF) return true
  return false
}
