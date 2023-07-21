package io.tolgee.service.machineTranslation

import io.tolgee.component.machineTranslation.MtServiceManager
import io.tolgee.component.machineTranslation.TranslateResult
import io.tolgee.component.machineTranslation.metadata.ExampleItem
import io.tolgee.component.machineTranslation.metadata.Metadata
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.constants.MtServiceType
import io.tolgee.events.OnAfterMachineTranslationEvent
import io.tolgee.events.OnBeforeMachineTranslationEvent
import io.tolgee.exceptions.BadRequestException
import io.tolgee.helpers.TextHelper
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.service.key.KeyService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.translation.TranslationService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class MtService(
  private val translationService: TranslationService,
  private val machineTranslationManager: MtServiceManager,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val projectService: ProjectService,
  private val mtServiceConfigService: MtServiceConfigService,
  private val tolgeeProperties: TolgeeProperties,
  private val bigMetaService: BigMetaService,
  private val keyService: KeyService
) {
  fun getMachineTranslations(
    key: Key,
    targetLanguage: Language,
    services: Set<MtServiceType>?
  ):
    Map<MtServiceType, TranslateResult?>? {
    val baseLanguage = projectService.getOrCreateBaseLanguage(key.project.id)!!
    val baseTranslationText = translationService.find(key, baseLanguage).orElse(null)?.text

    if (baseTranslationText.isNullOrBlank()) {
      return null
    }

    return getMachineTranslations(
      project = key.project,
      baseTranslationText = baseTranslationText,
      keyId = key.id,
      baseLanguage = baseLanguage,
      targetLanguage = targetLanguage,
      services = services
    )
  }

  fun getMachineTranslations(
    project: Project,
    baseTranslationText: String,
    targetLanguage: Language,
    services: Set<MtServiceType>?
  ): Map<MtServiceType, TranslateResult?>? {
    val baseLanguage = projectService.getOrCreateBaseLanguage(project.id)!!
    return getMachineTranslations(
      project = project,
      baseTranslationText = baseTranslationText,
      keyId = null,
      baseLanguage = baseLanguage,
      targetLanguage = targetLanguage,
      services = services
    )
  }

  fun getPrimaryMachineTranslations(key: Key, targetLanguages: List<Language>):
    List<TranslateResult?> {
    val baseLanguage = projectService.getOrCreateBaseLanguage(key.project.id)!!
    val baseTranslationText = translationService.find(key, baseLanguage).orElse(null)?.text
      ?: return targetLanguages.map { null }
    return getPrimaryMachineTranslations(key.project, baseTranslationText, key.id, baseLanguage, targetLanguages)
  }

  private fun getPrimaryMachineTranslations(
    project: Project,
    baseTranslationText: String,
    keyId: Long?,
    baseLanguage: Language,
    targetLanguages: List<Language>
  ): List<TranslateResult?> {
    publishBeforeEvent(project)

    checkTextLength(baseTranslationText)
    val primaryServices = mtServiceConfigService.getPrimaryServices(targetLanguages.map { it.id }, project)
    val prepared = TextHelper.replaceIcuParams(baseTranslationText)

    val serviceIndexedLanguagesMap = targetLanguages
      .asSequence()
      .mapIndexed { idx, lang -> idx to lang }
      .groupBy { primaryServices[it.second.id] }

    val keyName = keyId?.let { projectService.keyService.get(it) }?.name

    val metadata = getMetadata(
      baseLanguage,
      targetLanguages.filter { primaryServices[it.id]?.usesMetadata == true },
      baseTranslationText,
      keyId,
      true,
    )

    val translationResults = serviceIndexedLanguagesMap
      .map { (service, languageIdxPairs) ->
        service?.let {
          val translateResults = machineTranslationManager.translate(
            prepared.text,
            baseTranslationText,
            keyName,
            baseLanguage.tag,
            languageIdxPairs.map { it.second.tag },
            service,
            metadata = metadata
          )

          val withReplacedParams = translateResults.map { translateResult ->
            translateResult.translatedText = translateResult.translatedText?.replaceParams(prepared.params)
            translateResult
          }
          languageIdxPairs.map { it.first }.zip(withReplacedParams)
        } ?: languageIdxPairs.map { it.first to null }
      }
      .flatten()
      .sortedBy { it.first }
      .map { it.second }

    val actualPrice = translationResults.sumOf { it?.actualPrice ?: 0 }

    publishAfterEvent(project, actualPrice)

    return translationResults
  }

  fun getMachineTranslations(
    project: Project,
    baseTranslationText: String,
    keyId: Long?,
    baseLanguage: Language,
    targetLanguage: Language,
    services: Set<MtServiceType>?
  ): Map<MtServiceType, TranslateResult?>? {
    publishBeforeEvent(project)

    checkTextLength(baseTranslationText)
    val enabledServices = mtServiceConfigService.getEnabledServices(targetLanguage.id)
    checkServices(desired = services, enabled = enabledServices)
    val servicesToUse = services ?: enabledServices
    val prepared = TextHelper.replaceIcuParams(baseTranslationText)

    val anyNeedsMetadata = enabledServices.any { it.usesMetadata }

    val metadata =
      getMetadata(baseLanguage, targetLanguage, baseTranslationText, keyId, anyNeedsMetadata)

    val keyName = keyId?.let { keyService.get(it) }?.name

    val results = machineTranslationManager
      .translateUsingAll(
        text = prepared.text,
        textRaw = baseTranslationText,
        keyName = keyName,
        sourceLanguageTag = baseLanguage.tag,
        targetLanguageTag = targetLanguage.tag,
        services = servicesToUse,
        metadata = metadata
      )

    val actualPrice = results.entries.sumOf { it.value.actualPrice }

    publishAfterEvent(project, actualPrice)

    return results.map { (serviceName, translated) ->
      translated.translatedText = translated.translatedText?.replaceParams(prepared.params)
      serviceName to translated
    }.toMap()
  }

  private fun checkServices(desired: Set<MtServiceType>?, enabled: List<MtServiceType>) {
    if (desired != null && desired.any { !enabled.contains(it) }) {
      throw BadRequestException(Message.MT_SERVICE_NOT_ENABLED)
    }
  }

  private fun publishAfterEvent(
    project: Project,
    actualPrice: Int
  ) {
    applicationEventPublisher.publishEvent(
      OnAfterMachineTranslationEvent(this, project, actualPrice)
    )
  }

  private fun publishBeforeEvent(
    project: Project,
  ) {
    applicationEventPublisher.publishEvent(
      OnBeforeMachineTranslationEvent(this, project)
    )
  }

  private fun checkTextLength(text: String) {
    if (text.length > tolgeeProperties.maxTranslationTextLength) {
      throw BadRequestException(Message.TRANSLATION_TEXT_TOO_LONG)
    }
  }

  private fun String.replaceParams(params: Map<String, String>): String {
    var replaced = this
    params.forEach { (placeholder, text) ->
      replaced = replaced.replace(placeholder, text)
    }
    return replaced
  }

  private fun getExamples(
    sourceLanguage: Language,
    targetLanguage: Language,
    text: String,
    keyId: Long?
  ): List<ExampleItem> {
    return translationService.getTranslationMemorySuggestions(
      sourceTranslationText = text,
      key = null,
      sourceLanguage = sourceLanguage,
      targetLanguage = targetLanguage,
      pageable = PageRequest.of(0, 5)
    ).content
      .filter { it.keyId != keyId }
      .map {
        ExampleItem(key = it.keyName, source = it.baseTranslationText, target = it.targetTranslationText)
      }
  }

  private fun getCloseItems(
    sourceLanguage: Language,
    targetLanguage: Language,
    closeKeyIds: List<Long>,
    keyId: Long?
  ): List<ExampleItem> {

    val translations = this.translationService.findAllByKeyIdsAndLanguageIds(
      closeKeyIds,
      languageIds = listOf(sourceLanguage.id, targetLanguage.id)
    )

    val sourceTranslations = translations.filter { it.language.id == sourceLanguage.id }

    val targetTranslations = translations.filter { it.language.id == targetLanguage.id }

    return sourceTranslations
      .filter { !it.text.isNullOrEmpty() }
      .map {
        ExampleItem(
          key = it.key.name,
          source = it.text ?: "",
          target = if (it.key.id != keyId) {
            targetTranslations.find { target -> target.key.id == it.key.id }?.text ?: ""
          } else ""
        )
      }
  }

  private fun getMetadata(
    sourceLanguage: Language,
    targetLanguages: List<Language>,
    text: String,
    keyId: Long?,
    needsMetadata: Boolean,
  ): Map<String, Metadata>? {
    if (!needsMetadata) {
      return null
    }

    val closeKeyIds = keyId?.let { bigMetaService.getCloseKeyIds(it) }

    return targetLanguages.associate { targetLanguage ->
      targetLanguage.tag to
        Metadata(
          examples = getExamples(sourceLanguage, targetLanguage, text, keyId),
          closeItems = closeKeyIds?.let { getCloseItems(sourceLanguage, targetLanguage, it, keyId) } ?: listOf(),
        )
    }
  }

  private fun getMetadata(
    sourceLanguage: Language,
    targetLanguages: Language,
    text: String,
    keyId: Long?,
    needsMetadata: Boolean = true,
  ): Metadata? {
    return getMetadata(sourceLanguage, listOf(targetLanguages), text, keyId, needsMetadata)?.get(
      targetLanguages.tag
    )
  }
}
