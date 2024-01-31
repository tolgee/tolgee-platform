package io.tolgee.formats

import io.tolgee.formats.po.`in`.ParsedCLikeParam
import io.tolgee.formats.po.`in`.ToIcuParamConvertor

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

fun convertMessage(
  message: String,
  isPlural: Boolean,
  convertorFactory: () -> ToIcuParamConvertor,
): String {
  val convertor = convertorFactory()
  return message.replaceMatchedAndUnmatched(
    string = message,
    regex = convertor.regex,
    matchedCallback = {
      convertor.convert(it)
    },
    unmatchedCallback = {
      PreIcuMessageEscaper(it, isPlural).escaped
    },
  )
}
