package io.tolgee.formats

import com.ibm.icu.impl.locale.LanguageTag
import com.ibm.icu.util.ULocale
import io.tolgee.component.machineTranslation.LanguageTagConvertor
import io.tolgee.formats.pluralData.PluralData
import io.tolgee.formats.pluralData.PluralLanguage

fun getULocaleFromTag(tag: String): ULocale {
  val suitableTag =
    LanguageTagConvertor.findSuitableTag(tag) { newTag ->
      LanguageTag.parse(newTag, null).extlangs.size == 0
    }
  return ULocale.forLanguageTag(suitableTag ?: "en")
}

fun getPluralData(languageTag: String): PluralLanguage {
  val locale = getULocaleFromTag(languageTag)
  return PluralData.DATA[locale.language] ?: throw NoPluralDataException(languageTag)
}

fun getPluralDataOrNull(locale: ULocale): PluralLanguage? {
  return PluralData.DATA[locale.language]
}

class NoPluralDataException(
  val languageTag: String,
) : RuntimeException("No plural data for language tag $languageTag")
