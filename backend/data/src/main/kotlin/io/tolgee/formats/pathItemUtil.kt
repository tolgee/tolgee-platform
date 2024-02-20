package io.tolgee.formats

interface PathItem

data class ArrayPathItem(val index: Int) : PathItem

data class ObjectPathItem(val value: String) : PathItem

enum class State {
  NORMAL,
  IN_ESCAPE,
  IN_BRACKETS,
}

fun getPathItems(
  path: String,
  disableArraySupport: Boolean = false,
  structureDelimiter: Char? = '.',
): MutableList<PathItem> {
  val items = mutableListOf<PathItem>()
  val buffer = StringBuilder()
  var state = State.NORMAL
  var itemValue = ""
  var bracketCount = 0

  for (ch in path) {
    when (state) {
      State.NORMAL ->
        when (ch) {
          structureDelimiter -> {
            itemValue = buffer.toString()
            buffer.clear()
            items.add(ObjectPathItem(itemValue))
          }

          '[' -> {
            if (disableArraySupport) {
              buffer.append(ch)
            } else {
              itemValue = buffer.toString()
              buffer.clear()
              bracketCount = 1

              if (itemValue.isNotEmpty()) {
                items.add(ObjectPathItem(itemValue))
              }

              state = State.IN_BRACKETS
            }
          }

          '\\' -> state = State.IN_ESCAPE
          else -> buffer.append(ch)
        }

      State.IN_ESCAPE -> {
        buffer.append(ch)
        state = State.NORMAL
      }

      State.IN_BRACKETS ->
        when (ch) {
          ']' -> {
            val index = buffer.toString().toInt()
            buffer.clear()

            items.add(ArrayPathItem(index))

            bracketCount--
            if (bracketCount == 0) {
              state = State.NORMAL
            }
          }

          '[' -> bracketCount++
          else -> buffer.append(ch)
        }
    }
  }

  itemValue = buffer.toString()
  if (itemValue.isNotEmpty()) {
    items.add(ObjectPathItem(itemValue))
  }
  return items
}

fun buildPath(
  items: List<PathItem>,
  structureDelimiter: Char? = '.',
): String {
  val path = StringBuilder()
  for (i in items.indices) {
    when (val item = items[i]) {
      is ObjectPathItem -> {
        val escapedValue =
          item.value
            .replace("\\", "\\\\")
            .replace("$structureDelimiter", "\\$structureDelimiter")
            .replace("[", "\\[")
            .replace("]", "\\]")
        path.append(escapedValue)
      }

      is ArrayPathItem -> path.append("[${item.index}]")
    }

    // Add dot separator if the next item is not ArrayPathItem
    if (i < items.size - 1 && items[i + 1] !is ArrayPathItem) {
      path.append('.')
    }
  }
  return path.toString()
}
