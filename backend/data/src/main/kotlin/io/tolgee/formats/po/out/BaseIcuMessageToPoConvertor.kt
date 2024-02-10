package io.tolgee.formats.po.out

import com.ibm.icu.text.PluralRules
import com.ibm.icu.text.PluralRules.FixedDecimal
import com.ibm.icu.util.ULocale
import io.tolgee.formats.BaseIcuMessageConvertor
import io.tolgee.formats.FromIcuParamConvertor
import io.tolgee.formats.getPluralDataOrNull
import io.tolgee.formats.getULocaleFromTag
import io.tolgee.formats.pluralData.PluralData

class BaseIcuMessageToPoConvertor(
  val message: String,
  val argumentConverter: FromIcuParamConvertor,
  val languageTag: String = "en",
) {
  private val cLikeConvertor = BaseIcuMessageConvertor(message, argumentConverter)

  companion object {
    const val OTHER_KEYWORD = "other"
  }

  private val locale: ULocale by lazy {
    getULocaleFromTag(languageTag)
  }

  private val languagePluralData by lazy {
    getPluralDataOrNull(locale) ?: let {
      PluralData.DATA["en"]!!
    }
  }

  fun convert(): ToPoConversionResult {
    val possiblePluralResult = cLikeConvertor.convert()
    if (!possiblePluralResult.isPlural()) {
      return ToPoConversionResult(possiblePluralResult.singleResult, null, possiblePluralResult.warnings)
    }

    val poPluralResult = getPluralResult(possiblePluralResult.formsResult ?: mutableMapOf())
    return ToPoConversionResult(null, poPluralResult, possiblePluralResult.warnings)
  }

  private fun getPluralResult(formsResult: Map<String, String>): List<String> {
    val forms = getPluralForms(formsResult)
    val plurals =
      languagePluralData.examples.map {
        val form = forms[it.plural] ?: OTHER_KEYWORD
        it.plural to (formsResult[form] ?: formsResult[OTHER_KEYWORD] ?: "")
      }.sortedBy { it.first }.map { it.second }.toList()

    return plurals
  }

  private fun getPluralForms(pluralFormsResult: Map<String, String>): Map<Int, String> {
    val pluralIndexes =
      pluralFormsResult
        .map { it.key to getPluralIndexesForKeyword(it.key) }.toMap()

    val allIndexes = pluralIndexes.flatMap { it.value }.toSet()
    return allIndexes.mapNotNull { index ->
      val keyword =
        pluralIndexes.entries
          // We need to find keyword which contains only this index, because "other" keyword matches all
          .find { entry -> entry.value.contains(index) && entry.value.size == 1 }?.key
          ?: pluralIndexes.entries.find { entry ->
            entry.value.contains(index)
          }?.key ?: return@mapNotNull null
      index to keyword
    }.toMap()
  }

  private fun getPluralIndexesForKeyword(keyword: String) =
    languagePluralData.examples.filter {
      // This is probably only way how to do it, so we have to use internal API
      @Suppress("DEPRECATION")
      PluralRules.forLocale(locale).matches(FixedDecimal(it.sample.toString()), keyword)
    }.map { it.plural }
}
