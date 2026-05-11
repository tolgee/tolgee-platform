package io.tolgee.ee.service.qa.checks

/**
 * Port of the frontend tag parser from editor/src/parser/placeholders/findTags.ts
 * and editor/src/parser/lezer/helpers.ts (isTagNameChar, isWhiteSpace).
 *
 * Extracts HTML-like tags from text using a state machine. Handles:
 * - Open tags: <b>, <a href="url">, <myComponent>
 * - Close tags: </b>, </myComponent>
 * - Self-closing tags: <br/>, <img />
 * - Tag attributes with quoted values (single or double quotes)
 * - Broad tag name characters from the ICU/formatjs spec
 */
object HtmlTagParser {
  /**
   * Source: formatjs/icu-messageformat-parser
   * Ported from editor/src/parser/lezer/helpers.ts
   */
  fun isTagNameChar(c: Int): Boolean =
    // '-'
    c == 0x2D ||
      // '.'
      c == 0x2E ||
      // 0..9
      c in 0x30..0x39 ||
      // '_'
      c == 0x5F ||
      // a..z
      c in 0x61..0x7A ||
      // A..Z
      c in 0x41..0x5A ||
      c == 0xB7 ||
      c in 0xC0..0xD6 ||
      c in 0xD8..0xF6 ||
      c in 0xF8..0x37D ||
      c in 0x37F..0x1FFF ||
      c in 0x200C..0x200D ||
      c in 0x203F..0x2040 ||
      c in 0x2070..0x218F ||
      c in 0x2C00..0x2FEF ||
      c in 0x3001..0xD7FF ||
      c in 0xF900..0xFDCF ||
      c in 0xFDF0..0xFFFD ||
      c in 0x10000..0xEFFFF

  private fun isWhiteSpace(c: Int): Boolean =
    c in 0x0009..0x000D ||
      c == 0x0020 ||
      c == 0x0085 ||
      c in 0x200E..0x200F ||
      c == 0x2028 ||
      c == 0x2029

  fun findTags(text: String): List<HtmlTag> {
    val tags = mutableListOf<HtmlTag>()
    var pos = 0
    while (pos < text.length) {
      val ltPos = text.indexOf('<', pos)
      if (ltPos == -1) break

      val result = parseTag(text, ltPos)
      if (result != null) {
        tags.add(result)
        pos = result.end
      } else {
        pos = ltPos + 1
      }
    }
    return tags
  }

  private const val STATE_TAG_START = 1
  private const val STATE_TAG_NAME = 2
  private const val STATE_TAG_VALID = 4
  private const val STATE_SELF_CLOSING_END = 5
  private const val STATE_PARAMS_OR_END = 6
  private const val STATE_PARAM_NAME = 7
  private const val STATE_EQUAL_OR_END = 8
  private const val STATE_PARAM_VALUE_START = 9
  private const val STATE_PARAM_VALUE = 10

  /**
   * Attempts to parse a tag starting at [startPos], which must point to '<'.
   * Returns the parsed [HtmlTag] or null if the text at that position is not a valid tag.
   */
  private fun parseTag(
    text: String,
    startPos: Int,
  ): HtmlTag? {
    if (startPos >= text.length || text[startPos] != '<') return null

    var state = STATE_TAG_START
    var kind = HtmlTagKind.OPEN
    val tagName = StringBuilder()
    var paramQuoteChar = '\''
    var pos = startPos + 1

    loop@ while (pos < text.length) {
      val char = text[pos]
      val cp = char.code

      when (state) {
        STATE_TAG_START -> {
          if (char == '/') {
            kind = HtmlTagKind.CLOSE
            state = STATE_TAG_NAME
          } else if (isTagNameChar(cp)) {
            tagName.append(char)
            state = STATE_TAG_NAME
          } else {
            break@loop
          }
        }

        STATE_TAG_NAME -> {
          when {
            char == '>' -> {
              if (tagName.isEmpty()) break@loop
              state = STATE_TAG_VALID
            }
            char == '/' -> {
              if (tagName.isEmpty()) break@loop
              kind = HtmlTagKind.SELF_CLOSING
              state = STATE_SELF_CLOSING_END
            }
            isWhiteSpace(cp) -> {
              if (tagName.isEmpty()) break@loop
              state = STATE_PARAMS_OR_END
            }
            isTagNameChar(cp) -> tagName.append(char)
            else -> break@loop
          }
        }

        STATE_PARAMS_OR_END -> {
          when {
            isTagNameChar(cp) -> state = STATE_PARAM_NAME
            char == '/' -> {
              kind = HtmlTagKind.SELF_CLOSING
              state = STATE_SELF_CLOSING_END
            }
            char == '>' -> state = STATE_TAG_VALID
            // else: skip whitespace and other chars
          }
        }

        STATE_PARAM_NAME -> {
          when {
            isTagNameChar(cp) -> { /* continue */ }
            char == '>' -> state = STATE_TAG_VALID
            char == '/' -> {
              kind = HtmlTagKind.SELF_CLOSING
              state = STATE_SELF_CLOSING_END
            }
            char == '=' -> state = STATE_PARAM_VALUE_START
            isWhiteSpace(cp) -> state = STATE_EQUAL_OR_END
            else -> break@loop
          }
        }

        STATE_EQUAL_OR_END -> {
          when {
            char == '>' -> state = STATE_TAG_VALID
            char == '/' -> {
              kind = HtmlTagKind.SELF_CLOSING
              state = STATE_SELF_CLOSING_END
            }
            char == '=' -> state = STATE_PARAM_VALUE_START
            isTagNameChar(cp) -> state = STATE_PARAM_NAME
            isWhiteSpace(cp) -> { /* skip */ }
            else -> break@loop
          }
        }

        STATE_PARAM_VALUE_START -> {
          when {
            char == '\'' || char == '"' -> {
              paramQuoteChar = char
              state = STATE_PARAM_VALUE
            }
            isWhiteSpace(cp) -> { /* skip */ }
            else -> break@loop
          }
        }

        STATE_PARAM_VALUE -> {
          if (char == paramQuoteChar) {
            state = STATE_PARAMS_OR_END
          }
          // else: continue consuming param value
        }

        STATE_SELF_CLOSING_END -> {
          if (char == '>') {
            state = STATE_TAG_VALID
          } else {
            break@loop
          }
        }

        STATE_TAG_VALID -> break@loop
      }

      pos++
    }

    if (state == STATE_TAG_VALID && tagName.isNotEmpty()) {
      return HtmlTag(
        name = tagName.toString(),
        kind = kind,
        raw = text.substring(startPos, pos),
        start = startPos,
        end = pos,
      )
    }

    return null
  }
}

enum class HtmlTagKind {
  OPEN,
  CLOSE,
  SELF_CLOSING,
}

data class HtmlTag(
  val name: String,
  val kind: HtmlTagKind,
  val raw: String,
  val start: Int,
  val end: Int,
)
