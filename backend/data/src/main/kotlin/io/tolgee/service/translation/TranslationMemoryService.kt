package io.tolgee.service.translation

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.model.Language
import io.tolgee.model.key.Key
import io.tolgee.model.views.TranslationMemoryItemView
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TranslationMemoryService(
  private val translationsService: TranslationService,
  private val entityManager: EntityManager,
) {
  @Transactional
  fun getAutoTranslatedValue(
    key: Key,
    targetLanguage: Language,
  ): TranslationMemoryItemView? {
    return translationsService.getTranslationMemoryValue(key, targetLanguage)
  }

  @Transactional
  fun getSuggestions(
    key: Key,
    targetLanguage: LanguageDto,
    pageable: Pageable,
  ): Page<TranslationMemoryItemView> {
    val baseTranslation = translationsService.findBaseTranslation(key) ?: return Page.empty()

    val baseTranslationText = baseTranslation.text ?: return Page.empty(pageable)

    return getSuggestions(
      baseTranslationText,
      key.isPlural,
      key.id,
      targetLanguage,
      pageable,
    )
  }

  @Transactional
  fun getSuggestions(
    baseTranslationText: String,
    isPlural: Boolean,
    keyId: Long? = null,
    targetLanguage: LanguageDto,
    pageable: Pageable,
  ): Page<TranslationMemoryItemView> {
    val (count, data) =
      getSuggestionsData(baseTranslationText, isPlural, keyId, targetLanguage, pageable.offset, pageable.pageSize)
    return PageImpl(data, pageable, count)
  }

  @Transactional
  fun getSuggestionsData(
    sourceTranslationText: String,
    isPlural: Boolean,
    keyId: Long?,
    targetLanguage: LanguageDto,
    offset: Long = 0,
    limit: Int = 5,
  ): Pair<Long, List<TranslationMemoryItemView>> {
    entityManager.createNativeQuery("set pg_trgm.similarity_threshold to 0.5").executeUpdate()
    val queryResult =
      entityManager.createNativeQuery(
        """
        with base as (
            select target.text as targetTranslationText, baseTranslation.text as baseTranslationText,
              key.name as keyName, ns.name as keyNamespace, key.id as keyId, 
            similarity(baseTranslation.text, :baseTranslationText) as similarity
            from translation baseTranslation
            join key on baseTranslation.key_id = key.id
            left join namespace ns on key.namespace_id = ns.id
            join project p on key.project_id = p.id
            join translation target on
                  target.key_id = key.id and 
                  target.language_id = :targetLanguageId and
                  target.text <> '' and
                  target.text is not null
            join key targetKey on target.key_id = targetKey.id    
            """ +

          // we use the case when syntax to force postgres to evaluate all the other conditions first,
          // the similarity condition is slow even it uses index, and it tends to be evaluated first since
          // huge underestimation
          """
            where case when (baseTranslation.language_id = p.base_language_id and
              (cast(:key as bigint) is null or targetKey.id <> :key) and targetKey.is_plural = :isPlural)
              then baseTranslation.text % :baseTranslationText end
        ) select base.*, count(*) over() 
        from base
        order by base.similarity desc
    """,
      ).setParameter("baseTranslationText", sourceTranslationText)
        .setParameter("isPlural", isPlural)
        .setParameter("key", keyId)
        .setParameter("targetLanguageId", targetLanguage.id)
        .setMaxResults(limit)
        .setFirstResult(offset.toInt())
        .resultList

    val count = (queryResult.firstOrNull() as Array<*>?)?.get(6) as Long? ?: 0L
    return count to
      queryResult.map {
        it as Array<*>
        TranslationMemoryItemView(
          targetTranslationText = it[0] as String,
          baseTranslationText = it[1] as String,
          keyName = it[2] as String,
          keyNamespace = it[3] as String?,
          similarity = it[5] as Float,
          keyId = it[4] as Long,
        )
      }
  }
}
