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

  fun getPrimaryMachineTranslations(key: Key, targetLanguages: List<Language>):
    List<String?> {
    val baseLanguage = projectService.getOrCreateBaseLanguage(key.project.id)!!
    val baseTranslationText = translationService.find(key, baseLanguage).orElse(null)?.text
      ?: return targetLanguages.map { null }
    return getPrimaryMachineTranslations(key.project, baseTranslationText, baseLanguage, targetLanguages)
  }

  private fun getPrimaryMachineTranslations(
    project: Project,
    baseTranslationText: String,
    baseLanguage: Language,
    targetLanguages: List<Language>
  ): List<String?> {
    val primaryServices = mtServiceConfigService.getPrimaryServices(targetLanguages.map { it.id })
    val prepared = TextHelper.replaceIcuParams(baseTranslationText)

    return targetLanguages
      .asSequence()
      .mapIndexed { idx, lang -> idx to lang }
      .groupBy { primaryServices[it.second.id] }
      .also {
        val price = calculatePrice(it, prepared)
        publishOnBeforeEvent(prepared, project, price)
      }
      .map { (service, languageIdxPairs) ->
        service?.let {
          val translated = machineTranslationManager.translate(
            prepared.text,
            baseLanguage.tag,
            languageIdxPairs.map { it.second.tag },
            service
          )

          val withReplacedParams = translated.map { replaceParams(prepared.params, it) }
          languageIdxPairs.map { it.first }.zip(withReplacedParams)
        } ?: languageIdxPairs.map { it.first to null }
      }
      .flatten()
      .sortedBy { it.first }
      .map { it.second }
      .toList()
  }

  private fun calculatePrice(
    it: Map<MtServiceType?, List<Pair<Int, Language>>>,
    prepared: TextHelper.ReplaceIcuResult
  ) =
    it.entries.sumOf { (service, languageIdxPairs) ->
      service?.let { serviceNotNull ->
        languageIdxPairs.sumOf {
          machineTranslationManager.calculatePrice(
            text = prepared.text,
            serviceNotNull
          )
        }
      } ?: 0
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

    publishOnBeforeEvent(prepared, project, price)

    return machineTranslationManager
      .translateUsingAll(prepared.text, baseLanguage.tag, targetLanguage.tag, enabledServices)
      .map { (serviceName, translated) ->
        var result = translated
        result = replaceParams(prepared.params, result)
        serviceName to result
      }.toMap()
  }

  private fun publishOnBeforeEvent(prepared: TextHelper.ReplaceIcuResult, project: Project, price: Int) {
    applicationEventPublisher.publishEvent(
      OnBeforeMachineTranslationEvent(this, prepared.text, project, price)
    )
  }

  private fun replaceParams(params: Map<String, String>, translated: String?): String? {
    var result = translated
    params.forEach { (placeholder, text) ->
      result = result?.replace(placeholder, text)
    }
    return result
  }
}
