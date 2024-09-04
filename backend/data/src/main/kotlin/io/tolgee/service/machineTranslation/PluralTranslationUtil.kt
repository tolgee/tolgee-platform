package io.tolgee.service.machineTranslation

import com.ibm.icu.text.PluralRules
import io.tolgee.formats.PluralForms
import io.tolgee.formats.getPluralFormExamples
import io.tolgee.formats.getULocaleFromTag
import io.tolgee.formats.toIcuPluralString

class PluralTranslationUtil(
  private val context: MtTranslatorContext,
  private val baseTranslationText: String,
  private val item: MtBatchItemParams,
  private val translateFn: (String) -> MtTranslatorResult,
) {
  fun translate(): MtTranslatorResult {
    return result
  }

  private val preparedFormSourceStrings: Sequence<Pair<String, String>> by lazy {
    val targetLanguageTag = context.getLanguage(item.targetLanguageId).tag
    val sourceLanguageTag = context.baseLanguage.tag
    getPreparedSourceStrings(sourceLanguageTag, targetLanguageTag, forms)
  }

  private val translated by lazy {
    preparedFormSourceStrings.map {
      it.first to translateFn(it.second)
    }
  }

  private val forms by lazy {
    context.getPluralFormsReplacingReplaceParam(baseTranslationText)
      ?: throw IllegalStateException("Plural forms are null")
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
          optimize = false,
        ),
      actualPrice = result.sumOf { it.second.actualPrice },
      contextDescription = result.firstOrNull { it.second.contextDescription != null }?.second?.contextDescription,
      service = item.service,
      targetLanguageId = item.targetLanguageId,
      baseBlank = false,
      exception = result.firstOrNull { it.second.exception != null }?.second?.exception,
    )
  }

  private fun String.replaceNumberTags(): String {
    return this.replace(TOLGEE_TAG_REGEX, "#")
  }

  companion object {
    const val REPLACE_NUMBER_PLACEHOLDER = "{%{REPLACE_NUMBER}%}"
    private const val TOLGEE_TAG_OPEN = "<x id=\"tolgee-number\">"
    private const val TOLGEE_TAG_CLOSE = "</x>"
    val TOLGEE_TAG_REGEX = "$TOLGEE_TAG_OPEN.*?$TOLGEE_TAG_CLOSE".toRegex()

    /**
     * Returns all target forms with examples from source
     */
    fun getSourceExamples(
      sourceLanguageTag: String,
      targetLanguageTag: String,
      pluralForms: PluralForms,
    ): Map<String, String> {
      return getSourceExamplesSequence(sourceLanguageTag, targetLanguageTag, pluralForms).toMap()
    }

    private fun getSourceExamplesSequence(
      sourceLanguageTag: String,
      targetLanguageTag: String,
      pluralForms: PluralForms,
    ): Sequence<Pair<String, String>> {
      return getTargetNumberExamples(targetLanguageTag).asSequence().map {
        val form = getRulesByTag(sourceLanguageTag)?.select(it.value.toDouble())
        val formValue = pluralForms.forms[form] ?: pluralForms.forms[PluralRules.KEYWORD_OTHER] ?: ""
        it.key to formValue.replaceReplaceNumberPlaceholderWithExample(it.value, addTag = false)
      }
    }

    private fun String.replaceReplaceNumberPlaceholderWithExample(
      example: Number,
      addTag: Boolean = true,
    ): String {
      val tagOpenString = if (addTag) TOLGEE_TAG_OPEN else ""
      val tagCloseString = if (addTag) TOLGEE_TAG_CLOSE else ""
      return this.replace(
        REPLACE_NUMBER_PLACEHOLDER,
        "$tagOpenString${example}$tagCloseString",
      )
    }

    private fun getTargetNumberExamples(targetLanguageTag: String): Map<String, Number> {
      val targetULocale = getULocaleFromTag(targetLanguageTag)
      val targetRules = PluralRules.forLocale(targetULocale)
      return getPluralFormExamples(targetRules)
    }

    private fun getRulesByTag(languageTag: String): PluralRules? {
      val sourceULocale = getULocaleFromTag(languageTag)
      return PluralRules.forLocale(sourceULocale)
    }

    fun getPreparedSourceStrings(
      sourceLanguageTag: String,
      targetLanguageTag: String,
      forms: PluralForms,
    ): Sequence<Pair<String, String>> {
      val sourceRules = getRulesByTag(sourceLanguageTag)
      val keywordCases =
        getTargetExamples(targetLanguageTag).asSequence().map {
          val form = sourceRules?.select(it.value.toDouble())
          val formValue = forms.forms[form] ?: forms.forms[PluralRules.KEYWORD_OTHER] ?: ""
          it.key to formValue.replaceReplaceNumberPlaceholderWithExample(it.value)
        }

      val exactCases =
        forms.forms.asSequence().filter {
          it.key.startsWith("=")
        }.mapNotNull {
          val number = it.key.substring(1).toDoubleOrNull() ?: return@mapNotNull null
          it.key to it.value.replaceReplaceNumberPlaceholderWithExample(number)
        }

      return keywordCases + exactCases
    }

    private fun String.toDoubleOrNull(): Number? {
      return try {
        this.toBigDecimalOrNull()
      } catch (e: NumberFormatException) {
        null
      }
    }

    private fun getTargetExamples(targetLanguageTag: String): Map<String, Number> {
      val targetULocale = getULocaleFromTag(targetLanguageTag)
      val targetRules = PluralRules.forLocale(targetULocale)
      return getPluralFormExamples(targetRules)
    }
  }
}
