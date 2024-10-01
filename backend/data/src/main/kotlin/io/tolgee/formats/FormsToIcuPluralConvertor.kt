package io.tolgee.formats

import com.ibm.icu.text.PluralRules

class FormsToIcuPluralConvertor(
  val forms: Map<String, String>,
  val argName: String = DEFAULT_PLURAL_ARGUMENT_NAME,
  val optimize: Boolean = false,
  val addNewLines: Boolean,
) {
  fun convert(): String {
    val newLineStringInit = if (addNewLines) "\n" else " "
    val icuMsg = StringBuffer("{$argName, plural,$newLineStringInit")
    forms.let {
      if (PluralRules.KEYWORD_OTHER !in it) {
        return@let it + (PluralRules.KEYWORD_OTHER to "")
      }
      return@let it
    }.let {
      if (optimize) {
        return@let optimizePluralForms(it)
      }
      return@let it
    }.entries.forEachIndexed { index, (keyword, message) ->
      val isLast = index == forms.size - 1
      val newLineStringForm =
        if (addNewLines) {
          "\n"
        } else if (isLast) {
          ""
        } else {
          " "
        }

      icuMsg.append("$keyword {$message}$newLineStringForm")
    }
    icuMsg.append("}")
    return icuMsg.toString()
  }
}
