package io.tolgee.formats

import io.tolgee.formats.escaping.ForceIcuEscaper

class FormsToIcuPluralConvertor(
  val forms: Map<String, String>,
  val argName: String = DEFAULT_PLURAL_ARGUMENT_NAME,
  val forceEscape: Boolean = true,
  val optimize: Boolean = false,
  val addNewLines: Boolean,
) {
  fun convert(): String {
    val newLineStringInit = if (addNewLines) "\n" else " "
    val icuMsg = StringBuffer("{$argName, plural,$newLineStringInit")
    forms.let {
      if (optimize) {
        return@let optimizePluralForms(it)
      }
      return@let it
    }.entries.forEachIndexed { index, (keyword, message) ->
      val escaped = if (forceEscape) ForceIcuEscaper(message).escaped else message
      val isLast = index == forms.size - 1
      val newLineStringForm =
        if (addNewLines) {
          "\n"
        } else if (isLast) {
          ""
        } else {
          " "
        }

      icuMsg.append("$keyword {$escaped}$newLineStringForm")
    }
    icuMsg.append("}")
    return icuMsg.toString()
  }
}
