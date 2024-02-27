package io.tolgee.service.machineTranslation

import com.ibm.icu.text.PluralRules
import io.tolgee.formats.getPluralFormExamples
import io.tolgee.formats.getULocaleFromTag
import io.tolgee.formats.toIcuPluralString

class PluralTranslationUtil(
  private val context: MtTranslatorContext,
  private val baseTranslationText: String,
  private val item: MtBatchItemParams,
  private val translateFn: (String) -> MtTranslatorResult,
) {
  val forms by lazy {
    context.getPluralFormsReplacingReplaceParam(baseTranslationText)
      ?: throw IllegalStateException("Plural forms are null")
  }

  fun translate(): MtTranslatorResult {
    val prepared = this.prepareFormSourceStrings()
    val translated =
      prepared.map {
        it.key to translateFn(it.value)
      }.toMap()
    return getResult(translated)
  }

  private fun prepareFormSourceStrings(): Map<String, String> {
    val targetLanguageTag = context.getLanguage(item.targetLanguageId).tag
    val sourceLanguageTag = context.getBaseLanguage().tag
    val targetULocale = getULocaleFromTag(targetLanguageTag)
    val sourceULocale = getULocaleFromTag(sourceLanguageTag)
    val targetRules = PluralRules.forLocale(targetULocale)
    val sourceRules = PluralRules.forLocale(sourceULocale)
    val targetExamples = getPluralFormExamples(targetRules)

    return targetExamples.map {
      val form = sourceRules.select(it.value.toDouble())
      val formValue = forms.forms[form] ?: forms.forms[PluralRules.KEYWORD_OTHER] ?: ""
      it.key to formValue.replaceReplaceNumberPlaceholderWithExample(it.value)
    }.toMap()
  }

  private fun getResult(translated: Map<String, MtTranslatorResult>): MtTranslatorResult {
    val result =
      translated.map { (form, result) ->
        result.translatedText = result.translatedText?.replaceNumberTags()
        form to result
      }.toMap()

    val resultForms = result.mapValues { it.value.translatedText ?: "" }

    return MtTranslatorResult(
      translatedText =
        resultForms.toIcuPluralString(
          argName = forms.argName,
        ),
      actualPrice = result.values.sumOf { it.actualPrice },
      contextDescription = result.values.firstOrNull { it.contextDescription != null }?.contextDescription,
      service = item.service,
      targetLanguageId = item.targetLanguageId,
      baseBlank = false,
      exception = result.values.firstOrNull { it.exception != null }?.exception,
    )
  }

  private fun String.replaceNumberTags(): String {
    return this.replace(TOLGEE_TAG_REGEX, "#")
  }

  private fun String.replaceReplaceNumberPlaceholderWithExample(example: Number): String {
    return this.replace(
      REPLACE_NUMBER_PLACEHOLDER,
      "${TOLGEE_TAG_OPEN}${example}${TOLGEE_TAG_CLOSE}",
    )
  }

  companion object {
    const val REPLACE_NUMBER_PLACEHOLDER = "{%{REPLACE_NUMBER}%}"
    const val TOLGEE_TAG_OPEN = "<tolgee>"
    const val TOLGEE_TAG_CLOSE = "</tolgee>"
    val TOLGEE_TAG_REGEX = "<tolgee>.*?</tolgee>".toRegex()
  }
}
