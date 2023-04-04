package io.tolgee.service.machineTranslation

import io.tolgee.component.machineTranslation.MtServiceManager
import io.tolgee.component.machineTranslation.TranslateResult
import io.tolgee.component.machineTranslation.metadata.ExampleItem
import io.tolgee.component.machineTranslation.metadata.Metadata
import io.tolgee.constants.MtServiceType
import io.tolgee.events.OnAfterMachineTranslationEvent
import io.tolgee.events.OnBeforeMachineTranslationEvent
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.key.Key
import io.tolgee.model.keyBigMeta.SurroundingKey
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
  private val bigMetaService: BigMetaService,
  private val keyService: KeyService
) {
  fun getMachineTranslations(key: Key, targetLanguage: Language):
    Map<MtServiceType, String?>? {
    val baseLanguage = projectService.getOrCreateBaseLanguage(key.project.id)!!
    val sourceText = translationService.find(key, baseLanguage).orElse(null)?.text

    if (sourceText.isNullOrBlank()) {
      return null
    }

    return getMachineTranslations(key.project, sourceText, key.id, baseLanguage, targetLanguage)
  }

  fun getMachineTranslations(
    project: Project,
    sourceText: String,
    targetLanguage: Language
  ): Map<MtServiceType, String?>? {
    val baseLanguage = projectService.getOrCreateBaseLanguage(project.id)!!
    return getMachineTranslations(project, sourceText, null, baseLanguage, targetLanguage)
  }

  fun getPrimaryMachineTranslations(key: Key, targetLanguages: List<Language>):
    List<TranslateResult?> {
    val baseLanguage = projectService.getOrCreateBaseLanguage(key.project.id)!!
    val sourceText = translationService.find(key, baseLanguage).orElse(null)?.text
      ?: return targetLanguages.map { null }
    return getPrimaryMachineTranslations(key.project, sourceText, key.id, baseLanguage, targetLanguages)
  }

  private fun getPrimaryMachineTranslations(
    project: Project,
    sourceText: String,
    keyId: Long?,
    baseLanguage: Language,
    targetLanguages: List<Language>
  ): List<TranslateResult?> {
    val primaryServices = mtServiceConfigService.getPrimaryServices(targetLanguages.map { it.id }, project)

    val serviceIndexedLanguagesMap = targetLanguages
      .asSequence()
      .mapIndexed { idx, lang -> idx to lang }
      .groupBy { primaryServices[it.second.id] }

    val metadata = getMetadata(
      baseLanguage,
      targetLanguages.filter { primaryServices[it.id]?.usesMetadata == true },
      sourceText,
      keyId,
      true,
      project
    )

    val price = calculatePrice(baseLanguage, serviceIndexedLanguagesMap, sourceText, metadata)

    publishOnBeforeEvent(sourceText, project, price)

    val translationResults = serviceIndexedLanguagesMap.map { (service, languageIdxPairs) ->
      service?.let {
        val translateResults = machineTranslationManager.translate(
          sourceText,
          baseLanguage.tag,
          languageIdxPairs.map { it.second.tag },
          service,
          metadata = metadata
        )

        languageIdxPairs.map { it.first }.zip(translateResults)
      } ?: languageIdxPairs.map { it.first to null }
    }
      .flatten()
      .sortedBy { it.first }
      .map { it.second }

    val actualPrice = translationResults.sumOf { it?.actualPrice ?: 0 }

    publishOnAfterEvent(sourceText, project, price, actualPrice)

    return translationResults
  }

  private fun calculatePrice(
    sourceLanguage: Language,
    targetServiceLanguagesMap: Map<MtServiceType?, List<Pair<Int, Language>>>,
    text: String,
    metadata: Map<String, Metadata?>?
  ): Int {
    return targetServiceLanguagesMap.entries.sumOf { (service, languageIdxPairs) ->
      service?.let { serviceNotNull ->
        languageIdxPairs.sumOf { (_, targetLanguage) ->
          machineTranslationManager.calculatePrice(
            text = text,
            service = serviceNotNull,
            sourceLanguageTag = sourceLanguage.tag,
            targetLanguageTag = targetLanguage.tag,
            metadata = metadata?.get(targetLanguage.tag)
          )
        }
      } ?: 0
    }
  }

  fun getMachineTranslations(
    project: Project,
    sourceText: String,
    keyId: Long?,
    baseLanguage: Language,
    targetLanguage: Language
  ): Map<MtServiceType, String?>? {
    val enabledServices = mtServiceConfigService.getEnabledServices(targetLanguage.id)

    val anyNeedsMetadata = enabledServices.any { it.usesMetadata }

    val metadata =
      getMetadata(baseLanguage, targetLanguage, sourceText, keyId, anyNeedsMetadata, project)

    val expectedPrice = machineTranslationManager.calculatePriceAll(
      text = sourceText,
      services = enabledServices,
      sourceLanguageTag = baseLanguage.tag,
      targetLanguageTag = targetLanguage.tag,
      metadata = metadata
    )

    publishOnBeforeEvent(sourceText, project, expectedPrice)

    val results = machineTranslationManager
      .translateUsingAll(
        text = sourceText,
        sourceLanguageTag = baseLanguage.tag,
        targetLanguageTag = targetLanguage.tag,
        services = enabledServices,
        metadata = metadata
      )

    val actualPrice = results.entries.sumOf { it.value.actualPrice }

    publishOnAfterEvent(sourceText, project, expectedPrice, actualPrice)

    return results.map { (serviceName, translated) ->
      serviceName to translated.translatedText
    }.toMap()
  }

  private fun publishOnBeforeEvent(text: String, project: Project, price: Int) {
    applicationEventPublisher.publishEvent(
      OnBeforeMachineTranslationEvent(this, text, project, price)
    )
  }

  private fun publishOnAfterEvent(
    text: String,
    project: Project,
    expectedPrice: Int,
    actualPrice: Int
  ) {
    applicationEventPublisher.publishEvent(
      OnAfterMachineTranslationEvent(this, text, project, expectedPrice, actualPrice)
    )
  }

  private fun getExamples(
    sourceLanguage: Language,
    targetLanguage: Language,
    text: String,
    keyId: Long?
  ): List<ExampleItem> {
    return translationService.getTranslationMemorySuggestions(
      sourceText = text,
      key = null,
      sourceLanguage = sourceLanguage,
      targetLanguage = targetLanguage,
      pageable = PageRequest.of(0, 5)
    ).content
      .filter { it.keyId != keyId }
      .map {
        ExampleItem(
          key = it.keyName,
          source = it.baseTranslationText,
          target = it.targetTranslationText,
          namespace = it.keyNamespace
        )
      }
  }

  private fun getCloseItems(
    sourceLanguage: Language,
    targetLanguage: Language,
    keys: List<SurroundingKey>,
    project: Project,
    keyId: Long?
  ): List<ExampleItem> {

    val translations =
      translationService.getAll(project.id, keys, listOf(sourceLanguage, targetLanguage))
        .groupBy { it.language.id }
        .map { (langId, translations) ->
          langId to translations.associateBy { it.key.namespace?.name to it.key.name }
        }.toMap()

    return keys.mapNotNull { key ->
      val sourceTranslation = translations[sourceLanguage.id]?.get(key.namespace to key.name)

      // ignore self
      if (sourceTranslation?.key?.id == keyId) {
        return@mapNotNull null
      }

      ExampleItem(
        key = key.name,
        namespace = key.namespace,
        source = sourceTranslation?.text ?: "",
        target = translations[targetLanguage.id]?.get(key.namespace to key.name)?.text ?: "",
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

    val key = keyId?.let { keyService.find(it) }

    val bigMetaEntities = keyId?.let { bigMetaService.getAllForKey(it) } ?: emptyList()

    val keys = bigMetaEntities.mapNotNull { it.contextData }.flatten()

    return targetLanguages.associate { targetLanguage ->
      targetLanguage.tag to
        Metadata(
          examples = getExamples(sourceLanguage, targetLanguage, text, keyId),
          closeItems = getCloseItems(sourceLanguage, targetLanguage, keys, project, keyId),
          keyName = key?.name,
          keyNamespace = key?.namespace?.name
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
