package io.tolgee.formats.po.out

fun convertToPoMultilineString(
  keyword: String,
  text: String,
): String {
  // Normalize newlines and split the text on newline characters.
  val lines =
    text
      .replace("\r\n", "\n")
      .replace("\\", "\\\\")
      .replace("\"", "\\\"")
      .split("\n")

  if (lines.size == 1) {
    return "$keyword \"${lines[0]}\"\n"
  }

  return buildString {
    append("$keyword \"\"\n")
    for ((index, line) in lines.withIndex()) {
      if (index != lines.size - 1) {
        append("\"$line\\n\"\n")
      } else {
        append("\"$line\"\n")
      }
    }
  }
}
