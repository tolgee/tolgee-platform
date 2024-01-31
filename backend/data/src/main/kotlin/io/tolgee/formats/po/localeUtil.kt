package io.tolgee.formats.po

import com.ibm.icu.util.ULocale
import io.tolgee.formats.pluralData.PluralData
import io.tolgee.formats.pluralData.PluralLanguage

fun getLocaleFromTag(tag: String): ULocale {
  return ULocale.forLanguageTag(tag)
}

fun getPluralData(languageTag: String): PluralLanguage {
  val locale = getLocaleFromTag(languageTag)
  return PluralData.DATA[locale.language] ?: throw NoPluralDataException(languageTag)
}

fun getPluralData(locale: ULocale): PluralLanguage {
  return PluralData.DATA[locale.language] ?: throw NoPluralDataException(locale.toLanguageTag())
}

fun getPluralDataOrNull(locale: ULocale): PluralLanguage? {
  return PluralData.DATA[locale.language]
}

class NoPluralDataException(val languageTag: String) : RuntimeException("No plural data for language tag $languageTag")
