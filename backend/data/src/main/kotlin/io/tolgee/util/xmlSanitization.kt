package io.tolgee.util

/**
 * Strips every codepoint outside the XML 1.0 `Char` production. Apply at every user-content
 * boundary that lands in an XML document — both DOM and StAX writers throw on illegal codepoints
 * and a single bad one (e.g. a Word-pasted vertical tab) can sink the whole file.
 *
 * Walks codepoints rather than UTF-16 chars so valid surrogate pairs (emoji etc.) survive intact.
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
