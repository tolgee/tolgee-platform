package io.tolgee.formats

class FormsToIcuPluralConvertor(
  val forms: Map<String, String>,
  val argName: String = "0",
  val escape: Boolean = true,
  val optimize: Boolean = false,
) {
  fun convert(): String {
    val icuMsg = StringBuffer("{$argName, plural,\n")
    forms.let {
      if (optimize) {
        return@let optimizePluralForms(it)
      }
      return@let it
    }.entries.forEach { (keyword, message) ->
      val escaped = if (escape) IcuMessageEscaper(message, true).escaped else message
      icuMsg.append("$keyword {$escaped}\n")
    }
    icuMsg.append("}")
    return icuMsg.toString()
  }
}
