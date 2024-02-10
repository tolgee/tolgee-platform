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
  val forms = getPluralForms(string) ?: return string
  val optimizedForms = optimizePluralForms(forms.forms)
  return FormsToIcuPluralConvertor(optimizedForms, escape = false).convert()
}

/**
 * Returns all plural forms from the given ICU string
 * Returns null if the string is not a plural
 */
fun getPluralForms(string: String): PluralForms? {
  val converted =
    BaseIcuMessageConvertor(
      string,
      NoOpFromIcuParamConvertor(),
      keepEscaping = true,
    ).convert()

  return PluralForms(
    converted.formsResult ?: return null,
    converted.argName ?: throw IllegalStateException("Plural argument name not found"),
    converted.isWholeStringWrappedInPlural,
  )
}

data class PluralForms(
  val forms: Map<String, String>,
  val argName: String,
  val isWholeStringWrappedInPlural: Boolean,
)

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

fun String.convertToIcuPlural(): String {
  try {
    return this.normalizePlural()
  } catch (e: Exception) {
    // ignore errors, we will just escape everything and put it to other form
  }
  return allToOtherForm(this.preparePluralForm(escapeHash = true))
}

fun String.normalizePlural(): String {
  val forms =
    try {
      getPluralForms(this)
    } catch (e: Exception) {
      null
    } ?: throw StringIsNotPluralException()
  val preparedForms = forms.forms.mapValues { it.value.preparePluralForm(escapeHash = false) }
  return FormsToIcuPluralConvertor(preparedForms, forms.argName, escape = false).convert()
}

class StringIsNotPluralException : RuntimeException("String is not a plural")

private fun allToOtherForm(text: String): String {
  return "{value, plural, other {$text}}"
}

private fun String.preparePluralForm(escapeHash: Boolean = true): String {
  val result = StringBuilder()
  MessagePatternUtil.buildMessageNode(this).contents.forEach {
    if (it !is MessagePatternUtil.TextNode) {
      result.append(it.patternString)
      return@forEach
    }
    result.append(IcuMessageEscaper(it.patternString, escapeHash).escaped)
  }
  return result.toString()
}
