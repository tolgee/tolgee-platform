package io.tolgee.service.machineTranslation

import io.tolgee.component.machineTranslation.metadata.ExampleItem
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.service.translation.TranslationMemoryService
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Pageable

class MetadataProvider(
  private val context: MtTranslatorContext,
) {
  fun getCloseItems(
    sourceLanguage: LanguageDto,
    targetLanguage: LanguageDto,
    metadataKey: MetadataKey,
  ): List<ExampleItem> {
    val closeKeyIds = metadataKey.keyId?.let { bigMetaService.getCloseKeyIds(it) }

    return entityManager.createQuery(
      """
      select new 
         io.tolgee.component.machineTranslation.metadata.ExampleItem(source.text, target.text, key.name, ns.name) 
      from Translation source
      join source.key key on key.id <> :excludeKeyId
      left join key.namespace ns
      join key.translations target on target.language.id = :targetLanguageId
      where source.language.id = :sourceLanguageId 
          and key.id in :closeKeyIds 
          and key.id <> :excludeKeyId 
          and source.text is not null 
          and source.text <> '' 
          and target.text is not null 
          and target.text <> ''
    """,
      ExampleItem::class.java,
    )
      .setParameter("excludeKeyId", metadataKey.keyId)
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
    return translationMemoryService.getSuggestions(
      baseTranslationText = text,
      isPlural = isPlural,
      keyId = keyId,
      targetLanguage = targetLanguage,
      pageable = Pageable.ofSize(5),
    ).content.map {
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
}
