package io.tolgee.formats

import com.ibm.icu.text.PluralRules
import com.ibm.icu.util.ULocale
import java.util.Locale

private const val LATIN_SCRIPT = "Latn"

private val CASED_SCRIPTS = setOf(LATIN_SCRIPT, "Cyrl", "Grek", "Armn")

private val NOUN_CAPITALIZING_LANGUAGES = setOf("de", "lb")

fun isWrittenInCaselessScript(languageTag: String): Boolean {
  val script = likelyScript(languageTag)
  return script.isNotEmpty() && script !in CASED_SCRIPTS
}

fun isWrittenInNonLatinScript(languageTag: String): Boolean {
  val script = likelyScript(languageTag)
  return script.isNotEmpty() && script != LATIN_SCRIPT
}

fun capitalizesAllNouns(languageTag: String): Boolean {
  return languageCodeOf(languageTag) in NOUN_CAPITALIZING_LANGUAGES
}

fun languageCodeOf(languageTag: String): String {
  return localeOf(languageTag).language
}

fun pluralVariantMatchesSingleInteger(
  languageTag: String,
  variantKey: String,
): Boolean {
  if (variantKey.startsWith("=")) return true
  val rules = PluralRules.forLocale(localeOf(languageTag))
  return rules.getUniqueKeywordValue(variantKey) != PluralRules.NO_UNIQUE_VALUE
}

private fun likelyScript(languageTag: String): String {
  return ULocale.addLikelySubtags(localeOf(languageTag)).script
}

fun javaLocaleOf(languageTag: String): Locale {
  return localeOf(languageTag).toLocale()
}

/** Project language tags are free text and often use an underscore (`zh_CN`), which ICU does not parse. */
private fun localeOf(languageTag: String): ULocale {
  return getULocaleFromTag(languageTag.replace('_', '-'))
}
