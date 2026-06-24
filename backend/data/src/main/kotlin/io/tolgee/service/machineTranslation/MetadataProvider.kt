package io.tolgee.service.machineTranslation

import io.tolgee.component.machineTranslation.metadata.ExampleItem
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.service.translation.TranslationMemoryService
import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException

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

  /**
   * Builds a plain-text context payload for providers that accept one
   * Best-effort: context only improves the result, so a database failure while fetching the
   * neighbouring keys yields no context rather than failing the translation itself.
   */
  fun getContext(
    sourceLanguage: LanguageDto,
    targetLanguage: LanguageDto,
    metadataKey: MetadataKey,
    keyDescription: String?,
  ): String? {
    try {
      val parts = mutableListOf<String>()
      keyDescription?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
      getCloseItems(sourceLanguage, targetLanguage, metadataKey)
        .map { it.source }
        .filter { it.isNotBlank() }
        .forEach { parts.add(it) }
      return parts.joinToString("\n").ifBlank { null }
    } catch (e: DataAccessException) {
      log.warn("Failed to build MT context for key ${metadataKey.keyId}", e)
      return null
    }
  }

  /**
   * Examples carry the *penalized* similarity, so trust-adjusted TMs (e.g. a noisy imported TM
   * with a high default penalty) surface weaker context for MT prompts — matching the
   * user-facing suggestion ranking.
   */
  fun getExamples(
    targetLanguage: LanguageDto,
    isPlural: Boolean,
    text: String,
    keyId: Long?,
  ): List<ExampleItem> {
    val project = context.project
    return translationMemoryService
      .getSuggestionsList(
        baseTranslationText = text,
        isPlural = isPlural,
        keyId = keyId,
        projectId = project.id,
        organizationId = project.organizationOwnerId,
        targetLanguageTag = targetLanguage.tag,
        limit = 5,
      ).map {
        ExampleItem(
          key = it.keyName,
          keyNamespace = it.keyNamespace,
          source = it.baseTranslationText,
          target = it.targetTranslationText,
        )
      }
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

  private val mtGlossaryTermsProvider by lazy {
    context.applicationContext.getBean(MtGlossaryTermsProvider::class.java)
  }
}
