package io.tolgee.service.machineTranslation

import io.tolgee.component.machineTranslation.MtServiceManager
import io.tolgee.component.machineTranslation.TranslateResult
import io.tolgee.component.machineTranslation.TranslationParams
import io.tolgee.formats.PluralForms
import io.tolgee.formats.getPluralFormsForLocale
import io.tolgee.formats.toIcuPluralString
import io.tolgee.helpers.TextHelper
import org.springframework.context.ApplicationContext

class MtBatchTranslator(
  private val context: MtTranslatorContext,
  private val applicationContext: ApplicationContext,
) {
  fun translate(batch: List<MtBatchItemParams>): List<MtTranslatorResult> {
    val result = mutableListOf<MtTranslatorResult>()
    context.prepareKeys(batch)
    context.prepareMetadata(batch)

    batch.forEach { item ->
      result.add(translateItem(item))
    }

    return result
  }

  private fun translateItem(item: MtBatchItemParams): MtTranslatorResult {
    val baseTranslationText = item.baseTranslationText ?: context.keys[item.keyId]?.baseTranslation
    if (baseTranslationText == null) {
      return getEmptyResult(item)
    }

    if (isExistingKeyPlural(item) || isDetectedPlural(item.keyId, baseTranslationText)) {
      return translatePlural(item, baseTranslationText)
    }

    return translateInSingleRequest(item, baseTranslationText)
  }

  private fun translatePlural(
    item: MtBatchItemParams,
    baseTranslationText: String,
  ): MtTranslatorResult {
    if (item.service.supportsPlurals) {
      return translateInSingleRequest(item, baseTranslationText, context.getPluralForms(baseTranslationText))
    }
    return translatePluralSeparately(item, baseTranslationText)
  }

  private fun translatePluralSeparately(
    item: MtBatchItemParams,
    baseTranslationText: String,
  ): MtTranslatorResult {
    return PluralTranslationUtil(context, baseTranslationText, item) { prepared ->
      translateInSingleRequest(item, prepared)
    }.translate()
  }

  private fun isDetectedPlural(
    keyId: Long?,
    baseTranslationText: String,
  ): Boolean {
    if (keyId != null) {
      return false
    }
    return context.getPluralForms(baseTranslationText) != null
  }

  private fun isExistingKeyPlural(item: MtBatchItemParams) = context.keys[item.keyId]?.isPlural == true

  private fun translateInSingleRequest(
    item: MtBatchItemParams,
    baseTranslationText: String,
    pluralForms: PluralForms? = null,
  ): MtTranslatorResult {
    val withReplacedParams = TextHelper.replaceIcuParams(baseTranslationText)

    val managerResult =
      mtServiceManager.translate(
        getTranslationParams(
          item = item,
          baseTranslationText = baseTranslationText,
          withReplacedParams = withReplacedParams.text,
          pluralForms = pluralForms,
        ),
      )

    if (managerResult.translatedPluralForms != null && pluralForms != null) {
      return getPluralResult(managerResult, withReplacedParams, item, pluralForms.argName)
    }

    return managerResult.getTranslatorResult(withReplacedParams, item)
  }

  private fun getPluralResult(
    managerResult: TranslateResult,
    withReplacedParams: TextHelper.ReplaceIcuResult,
    item: MtBatchItemParams,
    argName: String,
  ): MtTranslatorResult {
    val forms =
      managerResult.translatedPluralForms
        ?: throw IllegalStateException("Plural forms are null")
    val translatedText = forms.toIcuPluralString(escape = false, optimize = true, argName = argName)
    return managerResult.getTranslatorResult(withReplacedParams, item).also {
      it.translatedText = translatedText
    }
  }

  private fun TranslateResult.getTranslatorResult(
    withReplacedParams: TextHelper.ReplaceIcuResult,
    item: MtBatchItemParams,
  ): MtTranslatorResult {
    return MtTranslatorResult(
      translatedText = translatedText?.replaceParams(withReplacedParams.params),
      actualPrice = actualPrice,
      contextDescription = contextDescription,
      service = item.service,
      targetLanguageId = item.targetLanguageId,
      baseBlank = baseBlank,
      exception = exception,
    )
  }

  private fun getEmptyResult(item: MtBatchItemParams) =
    MtTranslatorResult(
      translatedText = null,
      actualPrice = 0,
      contextDescription = null,
      service = item.service,
      targetLanguageId = item.targetLanguageId,
      baseBlank = true,
      exception = null,
    )

  private fun getTranslationParams(
    item: MtBatchItemParams,
    baseTranslationText: String,
    withReplacedParams: String,
    pluralForms: PluralForms?,
  ): TranslationParams {
    val targetLanguageTag =
      context.languages[item.targetLanguageId]?.tag
        ?: throw IllegalStateException("Language ${item.targetLanguageId} not found")

    val expectedPluralForms = pluralForms?.let { getPluralFormsForLocale(targetLanguageTag) }

    return TranslationParams(
      text = withReplacedParams,
      textRaw = baseTranslationText,
      keyName = context.keys[item.keyId]?.name,
      sourceLanguageTag = context.getBaseLanguage().tag,
      targetLanguageTag = targetLanguageTag,
      serviceInfo = context.getServiceInfo(item.targetLanguageId, item.service),
      metadata = context.getMetadata(item),
      isBatch = context.isBatch,
      pluralForms = pluralForms?.forms,
      expectedPluralForms = expectedPluralForms,
    )
  }

  private fun String.replaceParams(params: Map<String, String>): String {
    var replaced = this
    params.forEach { (placeholder, text) ->
      replaced = replaced.replace(placeholder, text)
    }
    return replaced
  }

  private val mtServiceManager: MtServiceManager by lazy {
    applicationContext.getBean(MtServiceManager::class.java)
  }
}
