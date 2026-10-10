package io.tolgee.service.machineTranslation

import com.ibm.icu.text.PluralRules
import io.tolgee.formats.PluralForms
import io.tolgee.formats.getPluralFormExamples
import io.tolgee.formats.getULocaleFromTag
import io.tolgee.formats.toIcuPluralString
import org.springframework.web.util.HtmlUtils

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
    getPreparedSourceStrings(sourceLanguageTag, targetLanguageTag, forms, item.service.escapesMarkup)
  }

  private val translated by lazy {
    val escaped = item.service.escapesMarkup
    preparedFormSourceStrings
      .map { (form, prepared) ->
        val result = translateFn(prepared)
        result.translatedText =
          result.translatedText?.let {
            restoreNumberPlaceholder(
              it,
              htmlEscaped = escaped && containsNumberTag(prepared),
            )
          }
        form to result
      }.toList()
  }

  private val forms by lazy {
    context.getPluralFormsReplacingReplaceParam(baseTranslationText)
      ?: throw IllegalStateException("Plural forms are null")
  }

  private val result: MtTranslatorResult by lazy {
    val result = translated

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
      promptId = item.promptId,
      exception = result.firstOrNull { it.second.exception != null }?.second?.exception,
    )
  }

  companion object {
    const val REPLACE_NUMBER_PLACEHOLDER = "{%{REPLACE_NUMBER}%}"
    const val TOLGEE_TAG_OPEN = "<x id=\"tolgee-number\">"
    private const val TOLGEE_TAG_CLOSE = "</x>"
    val TOLGEE_TAG_REGEX = "$TOLGEE_TAG_OPEN.*?$TOLGEE_TAG_CLOSE".toRegex()

    /**
     * Whether the text contains the [TOLGEE_TAG_OPEN] marker used to protect the ICU plural
     * "replace number" (`#`) placeholder while translating plural forms one by one. Providers must be
     * told (via their own tag-handling/HTML mode) to leave this tag untouched, otherwise the engine is
     * free to mangle or drop it, breaking [MtBatchTranslator] restoration of the `#` placeholder.
     */
    fun containsNumberTag(text: String): Boolean = text.contains(TOLGEE_TAG_OPEN)

    /**
     * Providers run in tag-handling mode when the number tag is present and then return `<`, `>`, `&`
     * and quotes as HTML entities, so the escaping applied in [replaceReplaceNumberPlaceholderWithExample]
     * has to be reversed here. Without it, `&lt;` ends up stored in the translation.
     */
    fun restoreNumberPlaceholder(
      translated: String,
      htmlEscaped: Boolean,
    ): String {
      if (!htmlEscaped) return translated.replace(TOLGEE_TAG_REGEX, "#")
      return unescapeMarkup(translated).replace(TOLGEE_TAG_REGEX, "#")
    }

    /**
     * Reverses [escapeMarkup] and nothing else, covering only the five characters it escapes in their
     * named, decimal and hex forms (plus XML `&apos;`, which an XML-mode engine may serialize instead).
     * A full HTML unescape would also decode entities the engine introduced on its own (`&nbsp;` into
     * U+00A0, say), which would then sit invisibly in the stored ICU string and break exact-match
     * comparison on re-import.
     */
    private val MARKUP_ENTITIES =
      listOf(
        "<" to listOf("&lt;", "&#60;", "&#x3C;"),
        ">" to listOf("&gt;", "&#62;", "&#x3E;"),
        "\"" to listOf("&quot;", "&#34;", "&#x22;"),
        "'" to listOf("&#39;", "&apos;", "&#x27;"),
        // must come last, so "&amp;lt;" decodes to "&lt;" and not to "<"
        "&" to listOf("&amp;", "&#38;", "&#x26;"),
      )

    private fun unescapeMarkup(text: String): String =
      MARKUP_ENTITIES.fold(text) { acc, (char, entities) ->
        entities.fold(acc) { inner, entity -> inner.replace(entity, char) }
      }

    private fun escapeMarkup(text: String): String = HtmlUtils.htmlEscape(text, Charsets.UTF_8.name())

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
      escapeMarkup: Boolean = false,
    ): String {
      if (!addTag) return this.replace(REPLACE_NUMBER_PLACEHOLDER, example.toString())
      if (!this.contains(REPLACE_NUMBER_PLACEHOLDER)) return this
      val body = if (escapeMarkup) escapeMarkup(this) else this
      return body.replace(
        REPLACE_NUMBER_PLACEHOLDER,
        "$TOLGEE_TAG_OPEN$example$TOLGEE_TAG_CLOSE",
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
      escapeMarkup: Boolean,
    ): Sequence<Pair<String, String>> {
      val sourceRules = getRulesByTag(sourceLanguageTag)
      val keywordCases =
        getTargetExamples(targetLanguageTag).asSequence().map {
          val form = sourceRules?.select(it.value.toDouble())
          val formValue = forms.forms[form] ?: forms.forms[PluralRules.KEYWORD_OTHER] ?: ""
          it.key to formValue.replaceReplaceNumberPlaceholderWithExample(it.value, escapeMarkup = escapeMarkup)
        }

      val exactCases =
        forms.forms
          .asSequence()
          .filter {
            it.key.startsWith("=")
          }.mapNotNull {
            val number = it.key.substring(1).toDoubleOrNull() ?: return@mapNotNull null
            it.key to it.value.replaceReplaceNumberPlaceholderWithExample(number, escapeMarkup = escapeMarkup)
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
