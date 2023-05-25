package io.tolgee.service.machineTranslation

import io.tolgee.component.machineTranslation.MtServiceManager
import io.tolgee.component.machineTranslation.TranslateResult
import io.tolgee.component.machineTranslation.metadata.ExampleItem
import io.tolgee.component.machineTranslation.metadata.Metadata
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.constants.MtServiceType
import io.tolgee.events.OnAfterMachineTranslationEvent
import io.tolgee.exceptions.BadRequestException
import io.tolgee.helpers.TextHelper
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.service.BigMetaService
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
  fun getMachineTranslations(key: Key, targetLanguage: Language):
    Map<MtServiceType, String?>? {
    val baseLanguage = projectService.getOrCreateBaseLanguage(key.project.id)!!
    val baseTranslationText = translationService.find(key, baseLanguage).orElse(null)?.text

    if (baseTranslationText.isNullOrBlank()) {
      return null
    }

    return getMachineTranslations(key.project, baseTranslationText, key.id, baseLanguage, targetLanguage)
  }

  fun getMachineTranslations(
    project: Project,
    baseTranslationText: String,
    targetLanguage: Language
  ): Map<MtServiceType, String?>? {
    val baseLanguage = projectService.getOrCreateBaseLanguage(project.id)!!
    return getMachineTranslations(project, baseTranslationText, null, baseLanguage, targetLanguage)
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
      project
    )

    val translationResults = serviceIndexedLanguagesMap.map { (service, languageIdxPairs) ->
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

    publishOnAfterEvent(prepared, project, actualPrice)

    return translationResults
  }

  fun getMachineTranslations(
    project: Project,
    baseTranslationText: String,
    keyId: Long?,
    baseLanguage: Language,
    targetLanguage: Language
  ): Map<MtServiceType, String?>? {
    checkTextLength(baseTranslationText)
    val enabledServices = mtServiceConfigService.getEnabledServices(targetLanguage.id)
    val prepared = TextHelper.replaceIcuParams(baseTranslationText)

    val anyNeedsMetadata = enabledServices.any { it.usesMetadata }

    val metadata =
      getMetadata(baseLanguage, targetLanguage, baseTranslationText, keyId, anyNeedsMetadata, project)

    val keyName = keyId?.let { keyService.get(it) }?.name

    val results = machineTranslationManager
      .translateUsingAll(
        text = prepared.text,
        textRaw = baseTranslationText,
        keyName = keyName,
        sourceLanguageTag = baseLanguage.tag,
        targetLanguageTag = targetLanguage.tag,
        services = enabledServices,
        metadata = metadata
      )

    val actualPrice = results.entries.sumOf { it.value.actualPrice }

    publishOnAfterEvent(prepared, project, actualPrice)

    return results.map { (serviceName, translated) ->
      serviceName to translated.translatedText?.replaceParams(prepared.params)
    }.toMap()
  }

  private fun publishOnAfterEvent(
    prepared: TextHelper.ReplaceIcuResult,
    project: Project,
    actualPrice: Int
  ) {
    applicationEventPublisher.publishEvent(
      OnAfterMachineTranslationEvent(this, prepared.text, project, actualPrice)
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
    keys: List<MetaMapper.Companion.MetaKey>,
    project: Project,
    keyId: Long?
  ): List<ExampleItem> {
    val keyNames = keys.map { it.keyName }
    val sourceTranslations = this.translationService.findAllByKey(
      keyNames,
      project,
      listOf(sourceLanguage)
    )

    val targetTranslations = this.translationService.findAllByKey(
      keyNames,
      project,
      listOf(targetLanguage)
    )

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
    project: Project
  ): Map<String, Metadata>? {
    if (!needsMetadata) {
      return null
    }

    val bigMetaEntities = keyId?.let { bigMetaService.getAllForKey(it) } ?: emptyList()

    val keys = bigMetaEntities.lastOrNull().let {
      val meta = (it?.contextData as? HashMap<*, *>)
      val metaJson = meta?.get("surroundingKeys")?.toString()
      metaJson?.let {
        val metaMapper = MetaMapper()
        metaMapper.getMeta(metaJson)
      } ?: listOf()
    }

    return targetLanguages.associate { targetLanguage ->
      targetLanguage.tag to
        Metadata(
          examples = getExamples(sourceLanguage, targetLanguage, text, keyId),
          closeItems = getCloseItems(sourceLanguage, targetLanguage, keys, project, keyId)
        )
    }
  }

  private fun getMetadata(
    sourceLanguage: Language,
    targetLanguages: Language,
    text: String,
    keyId: Long?,
    needsMetadata: Boolean = true,
    project: Project
  ): Metadata? {
    return getMetadata(sourceLanguage, listOf(targetLanguages), text, keyId, needsMetadata, project)?.get(
      targetLanguages.tag
    )
  }
}
