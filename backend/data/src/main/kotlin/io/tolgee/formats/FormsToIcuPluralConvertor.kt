package io.tolgee.formats

class FormsToIcuPluralConvertor(
  val forms: Map<String, String>,
) {
  fun convert(optimize: Boolean = false): String {
    val icuMsg = StringBuffer("{0, plural,\n")
    forms.let {
      if (optimize) {
        return@let optimizePluralForms(it)
      }
      return@let it
    }.entries.forEach { (keyword, message) ->
      icuMsg.append("$keyword {$message}\n")
    }
    icuMsg.append("}")
    return icuMsg.toString()
  }
}
