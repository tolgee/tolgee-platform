package io.tolgee.formats

import com.ibm.icu.text.PluralRules

private val POSSIBLE_MANY = arrayOf<Number>(6, 7, 8, 11, 20, 21, 1000000, 0.5, 0.1, 0.0)
private val POSSIBLE_FEW = arrayOf(0, 2, 3, 4, 6)
private val POSSIBLE_OTHER = arrayOf<Number>(10, 11, 20, 100, 0.0, 0, 0.1, 2, 3, 4)

private val KEYWORD_ZERO = "zero"
private val KEYWORD_ONE = "one"
private val KEYWORD_TWO = "two"
private val KEYWORD_FEW = "few"
private val KEYWORD_MANY = "many"
private val KEYWORD_OTHER = "other"

private val ALL_KEYWORDS =
  arrayOf(
    KEYWORD_ZERO,
    KEYWORD_ONE,
    KEYWORD_TWO,
    KEYWORD_FEW,
    KEYWORD_MANY,
    KEYWORD_OTHER,
  )

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
  return rules.orderedKeywords.associateWith {
    getVariantExample(rules, it)
  }
}

val PluralRules.orderedKeywords
  get() = keywords.toSortedSet { a, b -> ALL_KEYWORDS.indexOf(a) - ALL_KEYWORDS.indexOf(b) }

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
