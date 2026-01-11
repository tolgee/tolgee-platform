package io.tolgee.formats.path

import io.tolgee.util.nullIfEmpty

class PathParser(
  private val path: String,
  private val arraySupport: Boolean,
  private val structureDelimiter: Char? = '.',
) {
  private val items = mutableListOf<PathItem>()
  private val buffer = StringBuilderWithIndexes(path)
  private var state = State.INIT_ITEM
  private var itemValue = ""
  private var bracketCount = 0

  fun parse(): MutableList<PathItem> {
    path.forEachIndexed { index, ch ->
      handleChar(ch, index)
    }

    when (state) {
      State.POSSIBLE_ARRAY_END -> {
        handleArrayEnd(null, path.length)
      }

      State.INIT_ITEM -> {
        items.add(ObjectPathItem("", ""))
      }

      State.NORMAL -> {
        itemValue = buffer.toString()
        items.add(ObjectPathItem(itemValue, buffer.originalString()))
      }

      State.IN_ESCAPE -> {
        buffer.append('\\', path.length - 1)
        itemValue = buffer.toString()
        items.add(ObjectPathItem(itemValue, buffer.originalString()))
      }

      else -> {}
    }

    return items
  }

  private fun handleChar(
    ch: Char,
    index: Int,
  ) {
    when (state) {
      State.NORMAL, State.INIT_ITEM -> {
        when (ch) {
          structureDelimiter -> {
            itemValue = buffer.toString()
            items.add(ObjectPathItem(itemValue, buffer.originalString()))
            buffer.clear(index)
            state = State.INIT_ITEM
          }

          '[' -> {
            if (arraySupport) {
              bracketCount = 1
              state = State.IN_BRACKETS
            }
            buffer.append('[', index)
          }

          '\\' -> {
            when (isEscapingSupported) {
              true -> state = State.IN_ESCAPE
              false -> {
                state = State.NORMAL
                buffer.append('\\', index)
              }
            }
          }

          else -> {
            state = State.NORMAL
            buffer.append(ch, index)
          }
        }
      }

      State.IN_ESCAPE -> {
        if (!wasCharacterEscaped(ch)) {
          buffer.append('\\', index)
        }
        buffer.append(ch, index)
        state = State.NORMAL
      }

      State.IN_BRACKETS ->
        when (ch) {
          ']' -> {
            bracketCount--
            buffer.append(ch, index)
            if (bracketCount == 0) {
              state = State.POSSIBLE_ARRAY_END
            }
          }

          '[' -> {
            bracketCount++
            buffer.append(ch, index)
          }

          else -> buffer.append(ch, index)
        }

      State.POSSIBLE_ARRAY_END -> {
        when (ch) {
          structureDelimiter -> {
            handleArrayEnd(ch, index)
          }

          '[' -> {
            handleArrayEnd(ch, index)
          }

          else -> {
            state = State.NORMAL
            handleChar(ch, index)
          }
        }
      }
    }
  }

  private fun handleArrayEnd(
    ch: Char? = null,
    index: Int,
  ) {
    val bufferString = buffer.toString()
    val groups = indexParseRegex.matchEntire(bufferString)?.groups
    val indexGroup = groups?.get("index")?.value
    val maybeIndex = indexGroup?.toIntOrNull()
    val preArray = groups?.get("preArray")?.value
    if (maybeIndex != null) {
      val originalString = buffer.originalString()
      preArray?.nullIfEmpty?.let {
        val preArrayOriginal = originalString.substring(0, originalString.length - indexGroup.length - 2)
        items.add(ObjectPathItem(it, preArrayOriginal))
      }
      items.add(ArrayPathItem(maybeIndex, maybeIndex.toString()))
      buffer.clear(index)
    }

    if (ch == null) {
      return
    }

    state = State.NORMAL
    if (ch != structureDelimiter || maybeIndex == null) {
      handleChar(ch, index)
    }
  }

  private fun wasCharacterEscaped(ch: Char): Boolean {
    if (arraySupport && ch == '[') {
      return true
    }

    if (structureDelimiter != null && ch == structureDelimiter) {
      return true
    }

    if (ch == '\\') {
      return true
    }

    return false
  }

  val isEscapingSupported = arraySupport || structureDelimiter != null
}

private class StringBuilderWithIndexes(
  val path: String,
) {
  var start: Int = 0
  var end: Int = 0

  private val sb = StringBuilder()

  fun clear(index: Int) {
    this.start = index + 1
    this.end = index + 1
    sb.clear()
  }

  fun append(
    ch: Char,
    index: Int,
  ) {
    sb.append(ch)
    end = index + 1
  }

  override fun toString() = sb.toString()

  fun originalString(): String {
    return path.substring(start, end)
  }
}

val indexParseRegex by lazy {
  "^(?<preArray>.*)\\[(?<index>[0-9]+)\\]$".toRegex()
}

enum class State {
  INIT_ITEM,
  NORMAL,
  IN_ESCAPE,
  IN_BRACKETS,
  POSSIBLE_ARRAY_END,
}
