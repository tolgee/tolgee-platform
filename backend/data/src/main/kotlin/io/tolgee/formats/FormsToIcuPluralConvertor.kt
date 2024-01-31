package io.tolgee.formats

class FormsToIcuPluralConvertor(
  val forms: Map<String, String>,
) {
  fun convert(): String {
    val icuMsg = StringBuffer("{0, plural,\n")
    forms.entries.forEach { (keyword, message) ->
      icuMsg.append("$keyword {$message}\n")
    }
    icuMsg.append("}")
    return icuMsg.toString()
  }
}
