package io.tolgee.ee.service.qa.checks.lines

/**
 * Splits text into lines by CRLF, LF, or CR. Considers CRLF/CR/LF as separators NOT delimiters (e.g.
 * `\n` at the end of the text will result in an extra empty line).
 */
fun splitLines(text: String): List<Line> {
  val lines = mutableListOf<Line>()
  val buffer = StringBuilder()
  var offset = 0
  var pos = 0

  fun onLineEnd(type: SeparatorType) {
    lines.add(Line(buffer.toString(), offset, type))
    buffer.clear()
    offset = pos + 1
  }

  while (pos < text.length) {
    when (val ch = text[pos]) {
      '\n' -> {
        onLineEnd(SeparatorType.LF)
      }

      '\r' -> {
        if (text.length > pos + 1 && text[pos + 1] == '\n') {
          pos++
          onLineEnd(SeparatorType.CRLF)
        } else {
          onLineEnd(SeparatorType.CR)
        }
      }

      else -> {
        buffer.append(ch)
      }
    }
    pos++
  }

  onLineEnd(SeparatorType.UNKNOWN)
  return lines
}
