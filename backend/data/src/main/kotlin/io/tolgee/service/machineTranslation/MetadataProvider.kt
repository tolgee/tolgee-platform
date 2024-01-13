package io.tolgee.service.machineTranslation

import io.tolgee.component.machineTranslation.metadata.ExampleItem
import io.tolgee.component.machineTranslation.metadata.Metadata
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.service.translation.TranslationService
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.PageRequest

class MetadataProvider(
  private val context: MtTranslatorContext,
  private val applicationContext: ApplicationContext,
) {
  fun get(metadataKey: MetadataKey): Metadata {
    val closeKeyIds = metadataKey.keyId?.let { bigMetaService.getCloseKeyIds(it) }
    val keyDescription = context.keys[metadataKey.keyId]?.description

    val targetLanguage = context.getLanguage(metadataKey.targetLanguageId)

    return Metadata(
      examples =
        getExamples(
          targetLanguage,
          metadataKey.baseTranslationText,
          metadataKey.keyId,
        ),
      closeItems =
        closeKeyIds?.let {
          getCloseItems(
            context.getBaseLanguage(),
            targetLanguage,
            it,
            metadataKey.keyId,
          )
        } ?: listOf(),
      keyDescription = keyDescription,
      projectDescription = context.project.aiTranslatorPromptDescription,
      languageDescription = targetLanguage.aiTranslatorPromptDescription,
    )
  }

  private fun getCloseItems(
    sourceLanguage: LanguageDto,
    targetLanguage: LanguageDto,
    closeKeyIds: List<Long>,
    keyId: Long?,
  ): List<ExampleItem> {
    val translations =
      this.translationService.findAllByKeyIdsAndLanguageIds(
        closeKeyIds,
        languageIds = listOf(sourceLanguage.id, targetLanguage.id),
      )

    val sourceTranslations = translations.filter { it.language.id == sourceLanguage.id }

    val targetTranslations = translations.filter { it.language.id == targetLanguage.id }

    return sourceTranslations
      .filter { !it.text.isNullOrEmpty() }
      .map {
        ExampleItem(
          key = it.key.name,
          source = it.text ?: "",
          target =
            if (it.key.id != keyId) {
              targetTranslations.find { target -> target.key.id == it.key.id }?.text ?: ""
            } else {
              ""
            },
        )
      }
  }

  private fun getExamples(
    targetLanguage: LanguageDto,
    text: String,
    keyId: Long?,
  ): List<ExampleItem> {
    return translationService.getTranslationMemorySuggestions(
      sourceTranslationText = text,
      key = null,
      targetLanguage = targetLanguage,
      pageable = PageRequest.of(0, 5),
    ).content
      .filter { it.keyId != keyId }
      .map {
        ExampleItem(key = it.keyName, source = it.baseTranslationText, target = it.targetTranslationText)
      }
  }

  private val bigMetaService: BigMetaService by lazy {
    applicationContext.getBean(BigMetaService::class.java)
  }

  private val translationService: TranslationService by lazy {
    applicationContext.getBean(TranslationService::class.java)
  }
}
