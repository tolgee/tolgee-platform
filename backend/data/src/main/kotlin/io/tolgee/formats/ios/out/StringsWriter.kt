package io.tolgee.formats.ios.out

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
    content.append("\"${escaped(key)}\" = \"${escaped(value)}\";\n\n")
  }

  private fun escapeComment(s: String): String {
    return s.replace("*/", "*\\/")
  }

  private fun escaped(string: String): String {
    return string
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
  }

  val result: String
    get() = content.toString()
}
