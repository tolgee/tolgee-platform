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
    return result
  }

  private val preparedFormSourceStrings: Sequence<Pair<String, String>> by lazy {
    return@lazy targetExamples.asSequence().map {
      val form = sourceRules.select(it.value.toDouble())
      val formValue = forms.forms[form] ?: forms.forms[PluralRules.KEYWORD_OTHER] ?: ""
      it.key to formValue.replaceReplaceNumberPlaceholderWithExample(it.value)
    }
  }

  private val translated by lazy {
    preparedFormSourceStrings.map {
      it.first to translateFn(it.second)
    }
  }

  private val result: MtTranslatorResult by lazy {
    val result =
      translated.map { (form, result) ->
        result.translatedText = result.translatedText?.replaceNumberTags()
        form to result
      }

    val resultForms = result.map { it.first to (it.second.translatedText ?: "") }.toMap()

    return@lazy MtTranslatorResult(
      translatedText =
        resultForms.toIcuPluralString(
          argName = forms.argName,
        ),
      actualPrice = result.sumOf { it.second.actualPrice },
      contextDescription = result.firstOrNull { it.second.contextDescription != null }?.second?.contextDescription,
      service = item.service,
      targetLanguageId = item.targetLanguageId,
      baseBlank = false,
      exception = result.firstOrNull { it.second.exception != null }?.second?.exception,
    )
  }

  private val targetExamples by lazy {
    val targetLanguageTag = context.getLanguage(item.targetLanguageId).tag
    val targetULocale = getULocaleFromTag(targetLanguageTag)
    val targetRules = PluralRules.forLocale(targetULocale)
    getPluralFormExamples(targetRules)
  }

  private val sourceRules by lazy {
    val sourceLanguageTag = context.getBaseLanguage().tag
    val sourceULocale = getULocaleFromTag(sourceLanguageTag)
    PluralRules.forLocale(sourceULocale)
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
