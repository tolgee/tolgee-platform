package io.tolgee.service.machineTranslation

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.events.OnAfterMachineTranslationEvent
import io.tolgee.events.OnBeforeMachineTranslationEvent
import io.tolgee.exceptions.BadRequestException
import org.springframework.context.ApplicationContext

/**
 * This class is universal tool for translating using machine translation services.
 * It's designed to effectively target any amount of keys to any amount of languages using any services.
 * It uses the context for caching, so it's fetching only the required data only once, and it caches it for the whole
 * class life.
 */
class MtTranslator(
  projectId: Long,
  private val applicationContext: ApplicationContext,
  isBatch: Boolean,
) {
  private val context: MtTranslatorContext by lazy { MtTranslatorContext(projectId, applicationContext, isBatch) }

  fun translate(paramsList: List<MachineTranslationParams>): List<MtTranslatorResult> {
    validate(paramsList)
    publishBeforeEvent(context.project)
    context.preparePossibleTargetLanguages(paramsList)
    val batchItems = expandParams(paramsList)
    val result = MtBatchTranslator(context).translate(batchItems)
    publishAfterEvent(context.project, result.sumOf { it.actualPrice })
    return result
  }

  fun getBaseTranslation(
    keyId: Long?,
    baseTranslationText: String? = null,
  ): String? {
    if (keyId == null && baseTranslationText == null) {
      return null
    }
    return baseTranslationText ?: let {
      context.prepareKeysByIds(listOf(keyId!!))
      context.keys[keyId]?.baseTranslation
    }
  }

  fun getServicesToUseByDesiredServices(
    targetLanguageId: Long,
    desiredServices: Set<MtServiceType>?,
  ): Set<MtServiceInfo> {
    return context.getServicesToUseByDesiredServices(targetLanguageId, desiredServices)
  }

  /**
   * We can get request to translate to multiple languages or using multiple services,
   * This method expands such params to MtBatchItemParams, which hold only one language and one service per item
   */
  private fun expandParams(paramsList: List<MachineTranslationParams>): List<MtBatchItemParams> {
    val batchItems = mutableListOf<MtBatchItemParams>()

    paramsList.forEach { params ->
      val targetLanguages = params.allTargetLanguages()
      targetLanguages.forEach { targetLanguageId ->
        val servicesToUse = context.getServicesToUse(targetLanguageId, params)
        servicesToUse.forEach { serviceInfo ->
          batchItems.add(
            MtBatchItemParams(
              keyId = params.keyId,
              baseTranslationText = params.baseTranslationText ?: getBaseTranslation(params.keyId),
              targetLanguageId = targetLanguageId,
              service = serviceInfo.serviceType,
              promptId = serviceInfo.promptId,
            ),
          )
        }
      }
    }

    return batchItems
  }

  private val tolgeeProperties: TolgeeProperties by lazy {
    applicationContext.getBean(TolgeeProperties::class.java)
  }

  private fun checkTextLength(text: String?) {
    text ?: return
    if (text.length > tolgeeProperties.maxTranslationTextLength) {
      throw BadRequestException(Message.TRANSLATION_TEXT_TOO_LONG)
    }
  }

  private fun validate(params: List<MachineTranslationParams>) {
    params.forEach { it.baseTranslationText?.let { baseTranslationText -> checkTextLength(baseTranslationText) } }
  }

  private fun publishBeforeEvent(project: ProjectDto) {
    applicationContext.publishEvent(
      OnBeforeMachineTranslationEvent(this, project.organizationOwnerId),
    )
  }

  private fun publishAfterEvent(
    project: ProjectDto,
    actualPriceInCents: Int,
  ) {
    applicationContext.publishEvent(
      OnAfterMachineTranslationEvent(this, project.organizationOwnerId, actualPriceInCents),
    )
  }
}
