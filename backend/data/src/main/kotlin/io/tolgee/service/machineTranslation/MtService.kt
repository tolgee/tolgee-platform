package io.tolgee.service.machineTranslation

import io.tolgee.component.machineTranslation.MtServiceManager
import io.tolgee.constants.MtServiceType
import io.tolgee.events.OnBeforeMachineTranslationEvent
import io.tolgee.helpers.TextHelper
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.service.ProjectService
import io.tolgee.service.TranslationService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class MtService(
  private val translationService: TranslationService,
  private val machineTranslationManager: MtServiceManager,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val projectService: ProjectService,
  private val mtServiceConfigService: MtServiceConfigService
) {
  fun getMachineTranslations(key: Key, targetLanguage: Language):
    Map<MtServiceType, String?>? {
    val baseLanguage = projectService.getOrCreateBaseLanguage(key.project.id)!!
    val baseTranslationText = translationService.find(key, baseLanguage).orElse(null)?.text
      ?: return null
    return getMachineTranslations(key.project, baseTranslationText, baseLanguage, targetLanguage)
  }

  fun getMachineTranslations(
    project: Project,
    baseTranslationText: String,
    targetLanguage: Language
  ): Map<MtServiceType, String?>? {
    val baseLanguage = projectService.getOrCreateBaseLanguage(project.id)!!
    return getMachineTranslations(project, baseTranslationText, baseLanguage, targetLanguage)
  }

  fun getPrimaryMachineTranslation(key: Key, targetLanguage: Language):
    String? {
    val baseLanguage = projectService.getOrCreateBaseLanguage(key.project.id)!!
    val baseTranslationText = translationService.find(key, baseLanguage).orElse(null)?.text
      ?: return null
    return getPrimaryMachineTranslation(key.project, baseTranslationText, baseLanguage, targetLanguage)
  }

  fun getPrimaryMachineTranslation(
    project: Project,
    baseTranslationText: String,
    baseLanguage: Language,
    targetLanguage: Language
  ): String? {
    val service = mtServiceConfigService.getPrimaryService(targetLanguage.id)
    val prepared = TextHelper.replaceIcuParams(baseTranslationText)

    service?.let {
      val price = machineTranslationManager.calculatePrice(
        text = prepared.text,
        service
      )

      applicationEventPublisher.publishEvent(
        OnBeforeMachineTranslationEvent(this, prepared.text, project, price)
      )

      val translated = machineTranslationManager.translate(
        prepared.text,
        baseLanguage.tag,
        targetLanguage.tag,
        service
      )
      return replaceParams(prepared.params, translated)
    }
    return null
  }

  fun getMachineTranslations(
    project: Project,
    baseTranslationText: String,
    baseLanguage: Language,
    targetLanguage: Language
  ): Map<MtServiceType, String?>? {
    val enabledServices = mtServiceConfigService.getEnabledServices(targetLanguage.id)
    val prepared = TextHelper.replaceIcuParams(baseTranslationText)
    val price = machineTranslationManager.calculatePriceAll(
      text = prepared.text,
      services = enabledServices
    )

    applicationEventPublisher.publishEvent(
      OnBeforeMachineTranslationEvent(this, prepared.text, project, price)
    )

    return machineTranslationManager
      .translateUsingAll(prepared.text, baseLanguage.tag, targetLanguage.tag, enabledServices)
      .map { (serviceName, translated) ->
        var result = translated
        result = replaceParams(prepared.params, result)
        serviceName to result
      }.toMap()
  }

  private fun replaceParams(params: Map<String, String>, translated: String?): String? {
    var result = translated
    params.forEach { (placeholder, text) ->
      result = result?.replace(placeholder, text)
    }
    return result
  }
}
