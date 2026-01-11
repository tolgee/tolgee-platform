package io.tolgee.formats

import io.tolgee.formats.escaping.ForceIcuEscaper
import io.tolgee.formats.po.`in`.ParsedCLikeParam

/**
 * Handles the float conversion to ICU format
 * Return null if it cannot be converted reliably
 */
fun convertFloatToIcu(
  parsed: ParsedCLikeParam,
  name: String,
): String? {
  val precision = parsed.precision?.toLong() ?: 6
  val tooPrecise = precision > 50
  val usesUnsupportedFeature = usesUnsupportedFeature(parsed)
  if (tooPrecise || usesUnsupportedFeature) {
    return null
  }
  val precisionString = ".${(1..precision).joinToString("") { "0" }}"
  return "{$name, number, $precisionString}"
}

fun usesUnsupportedFeature(parsed: ParsedCLikeParam) =
  parsed.width != null || parsed.flags != null || parsed.length != null

fun convertMessage(
  message: String,
  isInPlural: Boolean,
  convertPlaceholders: Boolean,
  isProjectIcuEnabled: Boolean,
  escapeUnmatched: Boolean = true,
  convertorFactory: (() -> ToIcuPlaceholderConvertor)?,
): MessageConvertorResult {
  if (!isProjectIcuEnabled && !isInPlural) {
    return message.toConvertorResult()
  }
  if (!escapeUnmatched && convertorFactory == null) {
    return message.toConvertorResult()
  }
  if (!convertPlaceholders || convertorFactory == null) {
    return message.escapeIcu(isInPlural).toConvertorResult()
  }

  val convertor = convertorFactory()
  val converted =
    message.replaceMatchedAndUnmatched(
      regex = convertor.regex,
      matchedCallback = {
        convertor.convert(it, isInPlural)
      },
      unmatchedCallback = {
        if (escapeUnmatched) {
          ForceIcuEscaper(it, isInPlural).escaped
        } else {
          it
        }
      },
    )

  val pluralArgName = if (isInPlural) convertor.pluralArgName ?: DEFAULT_PLURAL_ARGUMENT_NAME else null
  return converted.toConvertorResult(pluralArgName)
}

private fun String?.toConvertorResult(pluralArgName: String? = null) = MessageConvertorResult(this, pluralArgName)
