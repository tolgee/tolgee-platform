package io.tolgee.formats.po.`in`.paramConvertors

import io.tolgee.formats.po.`in`.ParsedCLikeParam

fun convertFloatToIcu(
  parsed: ParsedCLikeParam,
  name: String,
): String {
  var precision = parsed.precision?.toLong() ?: 6
  if (precision > 50) {
    precision = 50
  }
  val precisionString = ".${(1..precision).joinToString("") { "0" }}"
  return "{$name, number, $precisionString}"
}
