package io.tolgee.formats

import com.ibm.icu.text.PluralRules

fun getPluralFormsForLocale(languageTag: String): MutableSet<String> {
  val uLocale = getULocaleFromTag(languageTag)
  val pluralRules = PluralRules.forLocale(uLocale)
  return pluralRules.keywords.sortedBy {
    formKeywords.indexOf(it)
  }.toMutableSet()
}

fun orderPluralForms(pluralForms: Map<String, String>): Map<String, String> {
  return pluralForms.entries.sortedBy {
    val formIndex = formKeywords.indexOf(it.key)
    if (formIndex == -1) {
      "A_$it"
    } else {
      formIndex.toString()
    }
  }.associate { it.key to it.value }
}

val formKeywords = listOf("zero", "one", "two", "few", "many", "other")

/**
 * It takes the plurals and optimizes them by removing the unnecessary forms
 * It also sorts it, so the strings can be compared and should be the same for the same forms
 */
fun optimizePossiblePlural(string: String): String {
  val converted = BaseIcuMessageToCLikeConvertor(string, NoOpFromIcuParamConvertor()).convert()
  if (!converted.isPlural()) {
    return string
  }
  val forms = converted.formsResult!!
  val optimizedForms = optimizePluralForms(forms)
  return FormsToIcuPluralConvertor(optimizedForms).convert()
}

fun optimizePluralForms(forms: Map<String, String>): Map<String, String> {
  val otherForm = forms[PluralRules.KEYWORD_OTHER] ?: return forms
  val filtered = forms.filter { it.key == PluralRules.KEYWORD_OTHER || it.value != otherForm }
  return orderPluralForms(filtered)
}

/**
 * It takes the plurals and optimizes them by removing the unnecessary forms
 * It also sorts it, so the strings can be compared and should be the same for the same forms
 */
infix fun String?.isSamePossiblePlural(other: String?): Boolean {
  if (this == other) {
    return true
  }
  if (this == null || other == null) {
    return false
  }
  return optimizePossiblePlural(this) == optimizePossiblePlural(other)
}
