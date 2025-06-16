package io.tolgee.service.machineTranslation

import io.tolgee.component.machineTranslation.TranslateResult
import io.tolgee.component.machineTranslation.TranslationParams
import io.tolgee.formats.DEFAULT_PLURAL_ARGUMENT_NAME
import io.tolgee.formats.forceEscapePluralForms
import io.tolgee.formats.toIcuPluralString
import io.tolgee.formats.unescapePluralForms
import io.tolgee.helpers.TextHelper

class MtBatchTranslator(
  private val context: MtTranslatorContext,
) {
  fun translate(batch: List<MtBatchItemParams>): List<MtTranslatorResult> {
    val result = mutableListOf<MtTranslatorResult>()
    context.prepareKeys(batch)

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
    val preparedText =
      if (context.project.icuPlaceholders) baseTranslationText else baseTranslationText.unescapePluralForms() ?: ""

    val translated =
      if (item.service.supportsPlurals) {
        translateInSingleRequest(item, preparedText, isPlural = true)
      } else {
        translatePluralSeparately(item, preparedText)
      }

    translated.translatedText =
      if (context.project.icuPlaceholders) {
        translated.translatedText
      } else {
        translated.translatedText?.forceEscapePluralForms()?.message
      }

    return translated
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
    isPlural: Boolean = false,
  ): MtTranslatorResult {
    val withReplacedParams = TextHelper.replaceIcuParams(baseTranslationText)

    val managerResult =
      context.mtServiceManager.translate(
        getTranslationParams(
          item = item,
          baseTranslationText = baseTranslationText,
          withReplacedParams = withReplacedParams.text,
          isPlural = isPlural,
        ),
      )

    if (managerResult.translatedPluralForms != null && isPlural) {
      return getPluralResult(
        managerResult,
        withReplacedParams,
        item,
        context.getPluralForms(baseTranslationText)?.argName ?: DEFAULT_PLURAL_ARGUMENT_NAME,
      )
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
    val translatedText = forms.toIcuPluralString(optimize = true, argName = argName)
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
      promptId = item.promptId,
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
      promptId = item.promptId,
      targetLanguageId = item.targetLanguageId,
      baseBlank = true,
      exception = null,
    )

  private fun getTranslationParams(
    item: MtBatchItemParams,
    baseTranslationText: String,
    withReplacedParams: String,
    isPlural: Boolean,
  ): TranslationParams {
    val targetLanguageTag =
      context.languages[item.targetLanguageId]?.tag
        ?: throw IllegalStateException("Language ${item.targetLanguageId} not found")

    val pluralForms = if (isPlural) context.getPluralForms(baseTranslationText) else null
    val pluralFormsWithReplacedParam =
      if (isPlural) context.getPluralFormsReplacingReplaceParam(baseTranslationText) else null

    val provider = context.applicationContext.getBean(item.service.providerClass)
    val metadata =
      provider.getMetadata(
        context.project.organizationOwnerId,
        context.project.id,
        item.keyId,
        item.targetLanguageId,
        item.promptId,
      )

    return TranslationParams(
      text = withReplacedParams,
      textRaw = baseTranslationText,
      keyName = context.keys[item.keyId]?.name,
      sourceLanguageTag = context.baseLanguage.tag,
      targetLanguageTag = targetLanguageTag,
      serviceInfo = context.getServiceInfo(item.targetLanguageId, item.service),
      metadata = metadata,
      isBatch = context.isBatch,
      pluralForms = pluralForms?.forms,
      pluralFormExamples =
        pluralFormsWithReplacedParam?.let {
          PluralTranslationUtil.getSourceExamples(
            context.baseLanguage.tag,
            targetLanguageTag,
            it,
          )
        },
    )
  }

  private fun String.replaceParams(params: Map<String, String>): String {
    var replaced = this
    params.forEach { (placeholder, text) ->
      replaced = replaced.replace(placeholder, text)
    }
    return replaced
  }
}
