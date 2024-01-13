package io.tolgee.service.machineTranslation

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.constants.MtServiceType
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.events.OnAfterMachineTranslationEvent
import io.tolgee.events.OnBeforeMachineTranslationEvent
import io.tolgee.exceptions.BadRequestException
import org.springframework.context.ApplicationContext

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
    val result = MtBatchTranslator(context, applicationContext).translate(batchItems)
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
    actualPrice: Int,
  ) {
    applicationContext.publishEvent(
      OnAfterMachineTranslationEvent(this, project.organizationOwnerId, actualPrice),
    )
  }
}
