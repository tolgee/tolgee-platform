package io.tolgee.service.machineTranslation

import io.tolgee.component.machineTranslation.metadata.ExampleItem
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.model.views.TranslationMemoryItemView
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.service.translation.TranslationMemoryService
import io.tolgee.service.translationMemory.ManagedTranslationMemorySuggestionService
import io.tolgee.service.translationMemory.TranslationMemoryManagementService
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.dao.QueryTimeoutException

class MetadataProvider(
  private val context: MtTranslatorContext,
) {
  private val log = LoggerFactory.getLogger(MetadataProvider::class.java)

  fun getCloseItems(
    sourceLanguage: LanguageDto,
    targetLanguage: LanguageDto,
    metadataKey: MetadataKey,
  ): List<ExampleItem> {
    val closeKeyIds = metadataKey.keyId?.let { bigMetaService.getCloseKeyIds(it) }

    return entityManager
      .createQuery(
        """
      select new 
         io.tolgee.component.machineTranslation.metadata.ExampleItem(source.text, target.text, key.name, ns.name) 
      from Translation source
      join source.key key on key.id <> :excludeKeyId
      left join key.namespace ns
      left join key.translations target on target.language.id = :targetLanguageId
      where source.language.id = :sourceLanguageId 
          and key.id in :closeKeyIds 
          and key.id <> :excludeKeyId 
          and source.text is not null 
          and source.text <> ''
    """,
        ExampleItem::class.java,
      ).setParameter("excludeKeyId", metadataKey.keyId)
      .setParameter("targetLanguageId", targetLanguage.id)
      .setParameter("sourceLanguageId", sourceLanguage.id)
      .setParameter("closeKeyIds", closeKeyIds)
      .resultList
  }

  fun getExamples(
    targetLanguage: LanguageDto,
    isPlural: Boolean,
    text: String,
    keyId: Long?,
  ): List<ExampleItem> {
    try {
      val results = fetchExamples(targetLanguage, isPlural, text, keyId)
      return results.map {
        ExampleItem(
          key = it.keyName,
          keyNamespace = it.keyNamespace,
          source = it.baseTranslationText,
          target = it.targetTranslationText,
        )
      }
    } catch (e: QueryTimeoutException) {
      log.warn("Translation memory suggestions timed out", e)
      return emptyList()
    }
  }

  /**
   * Routes the MT-context TM lookup through the managed service when the project has any
   * readable managed TMs (every project after the TM management feature ships).
   *
   * Examples carry the *penalized* similarity, so trust-adjusted TMs (e.g. a noisy
   * imported TM with a high default penalty) surface weaker context for MT prompts —
   * matching the user-facing suggestion ranking.
   *
   * Falls back to the legacy `translationMemoryService.getSuggestionsList` only for
   * projects that still lack a project TM (legacy free-plan projects predating the
   * auto-create in `ProjectCreationService`).
   */
  private fun fetchExamples(
    targetLanguage: LanguageDto,
    isPlural: Boolean,
    text: String,
    keyId: Long?,
  ): List<TranslationMemoryItemView> {
    val project = context.project
    val readableTmIds = translationMemoryManagementService.getReadableTmIds(project.id)
    if (readableTmIds.isNotEmpty()) {
      return managedTranslationMemorySuggestionService.getSuggestionsList(
        baseTranslationText = text,
        isPlural = isPlural,
        keyId = keyId,
        projectId = project.id,
        organizationId = project.organizationOwnerId,
        targetLanguageTag = targetLanguage.tag,
        limit = 5,
      )
    }
    return translationMemoryService.getSuggestionsList(
      baseTranslationText = text,
      isPlural = isPlural,
      keyId = keyId,
      baseLanguageId = context.baseLanguage.id,
      targetLanguage = targetLanguage,
      limit = 5,
    )
  }

  private val bigMetaService: BigMetaService by lazy {
    context.applicationContext.getBean(BigMetaService::class.java)
  }

  private val entityManager: EntityManager by lazy {
    context.applicationContext.getBean(EntityManager::class.java)
  }

  private val translationMemoryService: TranslationMemoryService by lazy {
    context.applicationContext.getBean(TranslationMemoryService::class.java)
  }

  private val translationMemoryManagementService: TranslationMemoryManagementService by lazy {
    context.applicationContext.getBean(TranslationMemoryManagementService::class.java)
  }

  private val managedTranslationMemorySuggestionService: ManagedTranslationMemorySuggestionService by lazy {
    context.applicationContext.getBean(ManagedTranslationMemorySuggestionService::class.java)
  }

  private val mtGlossaryTermsProvider by lazy {
    context.applicationContext.getBean(MtGlossaryTermsProvider::class.java)
  }
}
