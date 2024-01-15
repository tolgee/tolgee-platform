package io.tolgee.service.machineTranslation

import io.tolgee.component.machineTranslation.MtServiceManager
import io.tolgee.component.machineTranslation.TranslationParams
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

    val withReplacedParams = TextHelper.replaceIcuParams(baseTranslationText)

    val managerResult =
      mtServiceManager.translate(getTranslationParams(item, baseTranslationText, withReplacedParams.text))

    return MtTranslatorResult(
      translatedText = managerResult.translatedText?.replaceParams(withReplacedParams.params),
      actualPrice = managerResult.actualPrice,
      contextDescription = managerResult.contextDescription,
      service = item.service,
      targetLanguageId = item.targetLanguageId,
      baseBlank = managerResult.baseBlank,
      exception = managerResult.exception
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
      exception = null
    )

  private fun getTranslationParams(
    item: MtBatchItemParams,
    baseTranslationText: String,
    withReplacedParams: String,
  ): TranslationParams {
    return TranslationParams(
      text = withReplacedParams,
      textRaw = baseTranslationText,
      keyName = context.keys[item.keyId]?.name,
      sourceLanguageTag = context.getBaseLanguage().tag,
      targetLanguageTag =
      context.languages[item.targetLanguageId]?.tag
        ?: throw IllegalStateException("Language ${item.targetLanguageId} not found"),
      serviceInfo = context.getServiceInfo(item.targetLanguageId, item.service),
      metadata = context.getMetadata(item),
      isBatch = context.isBatch,
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
