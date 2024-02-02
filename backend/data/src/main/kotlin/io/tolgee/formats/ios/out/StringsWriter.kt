package io.tolgee.formats.ios.out

class StringsWriter {
  private val content = StringBuilder()

  fun addEntry(
    key: String,
    value: String,
  ) {
    content.append("\"${escaped(key)}\" = \"${escaped(value)}\";\n")
  }

  private fun escaped(string: String): String {
    return string
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
  }

  val result: String
    get() = content.toString()
}
