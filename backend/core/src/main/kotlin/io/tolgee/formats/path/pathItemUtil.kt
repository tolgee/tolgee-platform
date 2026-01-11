package io.tolgee.formats.path

interface PathItem {
  val originalPathString: String
}

class ArrayPathItem(
  var index: Int,
  override val originalPathString: String,
) : PathItem

class ObjectPathItem(
  var key: String,
  override val originalPathString: String,
) : PathItem

fun getPathItems(
  path: String,
  arraySupport: Boolean,
  structureDelimiter: Char? = '.',
): MutableList<PathItem> {
  return PathParser(path, arraySupport, structureDelimiter).parse()
}

fun buildPath(
  items: List<PathItem>,
  structureDelimiter: Char? = '.',
): String {
  val path = StringBuilder()
  for (i in items.indices) {
    when (val item = items[i]) {
      is ObjectPathItem -> {
        path.append(item.originalPathString)
      }

      is ArrayPathItem -> path.append("[${item.index}]")
    }

    // Add dot separator if the next item is not ArrayPathItem
    if (i < items.size - 1 && items[i + 1] !is ArrayPathItem) {
      path.append(structureDelimiter)
    }
  }
  return path.toString()
}
