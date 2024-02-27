package io.tolgee.formats

import com.ibm.icu.text.PluralRules

private val POSSIBLE_MANY = arrayOf<Number>(6, 7, 8, 11, 20, 21, 1000000, 0.5, 0.1, 0.0)
private val POSSIBLE_FEW = arrayOf(0, 2, 3, 4, 6)
private val POSSIBLE_OTHER = arrayOf<Number>(10, 11, 20, 100, 0.0, 0, 0.1, 2, 3, 4)

private fun findPluralFormExample(
  variant: String,
  rules: PluralRules,
  list: Array<out Number>,
): Number {
  return list.find {
    val double = it.toDouble()
    rules.select(double) == variant
  } ?: 10
}

fun getPluralFormExamples(languageTag: String): Map<String, Number> {
  val locale = getULocaleFromTag(languageTag)
  val rules = PluralRules.forLocale(locale)
  return getPluralFormExamples(rules)
}

fun getPluralFormExamples(rules: PluralRules): Map<String, Number> {
  return rules.keywords.associateWith {
    getVariantExample(rules, it)
  }
}

fun getVariantExample(
  rules: PluralRules,
  variant: String,
): Number {
  return when (variant) {
    "zero" -> findPluralFormExample("zero", rules, arrayOf(0))
    "one" -> findPluralFormExample("one", rules, arrayOf(1))
    "two" -> findPluralFormExample("two", rules, arrayOf(2))
    "few" -> findPluralFormExample("few", rules, POSSIBLE_FEW)
    "many" -> findPluralFormExample("many", rules, POSSIBLE_MANY)
    "other" -> findPluralFormExample("other", rules, POSSIBLE_OTHER)
    else -> variant.substring(1).toDoubleOrNull() ?: 10
  }
}
