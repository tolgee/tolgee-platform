package io.tolgee.service.translation

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.formats.BaseIcuMessageConvertor
import io.tolgee.formats.VisibleTextIcuPlaceholderConvertor
import io.tolgee.model.key.Key

fun validateCharLimit(
  key: Key,
  translations: Map<*, String?>,
) {
  val maxCharLimit = key.maxCharLimit ?: return
  if (maxCharLimit <= 0) return
  translations.values.forEach { text ->
    if (text != null && getMaxVisibleCharCount(text, key.isPlural) > maxCharLimit) {
      throw BadRequestException(
        Message.TRANSLATION_EXCEEDS_CHAR_LIMIT,
        listOf(key.name, maxCharLimit),
      )
    }
  }
}

private fun getMaxVisibleCharCount(
  text: String,
  isPlural: Boolean,
): Int {
  val result =
    BaseIcuMessageConvertor(
      message = text,
      argumentConvertorFactory = { VisibleTextIcuPlaceholderConvertor() },
      forceIsPlural = if (isPlural) true else null,
    ).convert()

  if (result.isPlural()) {
    return result.formsResult!!.values.maxOfOrNull { it.length } ?: 0
  }
  return result.singleResult?.length ?: 0
}
