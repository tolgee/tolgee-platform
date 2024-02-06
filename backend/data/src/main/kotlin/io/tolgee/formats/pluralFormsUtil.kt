package io.tolgee.formats

import com.ibm.icu.text.PluralRules
import java.util.*

fun getPluralFormsForLocale(languageTag: String): MutableSet<String> {
  val pluralRules = PluralRules.forLocale(Locale.forLanguageTag(languageTag))
  return pluralRules.keywords
}
