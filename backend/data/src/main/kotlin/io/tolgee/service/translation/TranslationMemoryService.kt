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
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class TranslationMemoryService(
  private val translationsService: TranslationService,
  private val entityManager: EntityManager,
) {
  /**
   * Returns translation memory suggestions for the batch/MT pipeline.
   *
   * Uses % (trigram threshold operator) without ORDER BY so the composite GiST index on
   * (language_id, text gist_trgm_ops) can stop early as soon as LIMIT rows are found,
   * avoiding a full scan of all matching rows.
   *
   * REQUIRES_NEW: isolates SET LOCAL PostgreSQL settings (statement_timeout,
   * pg_trgm.similarity_threshold) so they don't leak into the caller's transaction.
   * It also ensures a QueryTimeoutException does not mark the outer transaction rollback-only.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun getSuggestionsList(
    baseTranslationText: String,
    isPlural: Boolean,
    keyId: Long? = null,
    baseLanguageId: Long,
    targetLanguage: LanguageDto,
    limit: Int = 5,
  ): List<TranslationMemoryItemView> {
    // Protect against slow queries on large datasets. REQUIRES_NEW ensures this
    // timeout does not leak into the caller's transaction.
    entityManager.createNativeQuery("set local statement_timeout to '550ms'").executeUpdate()
    entityManager.createNativeQuery("set local pg_trgm.similarity_threshold to 0.5").executeUpdate()
    val queryResult =
      entityManager
        .createNativeQuery(
          """
        select target.text as targetTranslationText,
               baseTranslation.text as baseTranslationText,
               key.name as keyName, ns.name as keyNamespace, key.id as keyId,
               similarity(baseTranslation.text, :baseTranslationText) as similarity
        from translation baseTranslation
        join key on baseTranslation.key_id = key.id
        left join namespace ns on key.namespace_id = ns.id
        join translation target on
              target.key_id = key.id and
              target.language_id = :targetLanguageId and
              target.text <> '' and
              target.text is not null
        where baseTranslation.language_id = :baseLanguageId
          and (cast(:key as bigint) is null or key.id <> :key)
          and key.is_plural = :isPlural
          and baseTranslation.text % :baseTranslationText
    """,
        ).setParameter("baseTranslationText", baseTranslationText)
        .setParameter("isPlural", isPlural)
        .setParameter("key", keyId)
        .setParameter("baseLanguageId", baseLanguageId)
        .setParameter("targetLanguageId", targetLanguage.id)
        .setMaxResults(limit)
        .resultList

    return queryResult.map {
      it as Array<*>
      TranslationMemoryItemView(
        targetTranslationText = it[0] as String,
        baseTranslationText = it[1] as String,
        keyName = it[2] as String,
        keyNamespace = it[3] as String?,
        keyId = it[4] as Long,
        similarity = it[5] as Float,
      )
    }
  }

  @Transactional
  fun getAutoTranslatedValue(
    key: Key,
    targetLanguage: Language,
  ): TranslationMemoryItemView? {
    return translationsService.getTranslationMemoryValue(key, targetLanguage)
  }

  // REQUIRES_NEW: isolates SET LOCAL settings (statement_timeout, pg_trgm.similarity_threshold)
  // applied inside getSuggestionsData so they don't leak into the caller's transaction.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
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

  // REQUIRES_NEW: isolates SET LOCAL settings (statement_timeout, pg_trgm.similarity_threshold)
  // applied inside getSuggestionsData so they don't leak into the caller's transaction.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
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
    // Protect against slow queries on large datasets. The surrounding REQUIRES_NEW
    // transaction (from getSuggestions callers) ensures this timeout does not leak.
    entityManager.createNativeQuery("set local statement_timeout to '550ms'").executeUpdate()
    entityManager.createNativeQuery("set local pg_trgm.similarity_threshold to 0.5").executeUpdate()
    val queryResult =
      entityManager
        .createNativeQuery(
          """
        select target.text as targetTranslationText, baseTranslation.text as baseTranslationText,
               key.name as keyName, ns.name as keyNamespace, key.id as keyId,
               similarity(baseTranslation.text, :baseTranslationText) as similarity,
               count(*) over() as totalCount
        from translation baseTranslation
        join key on baseTranslation.key_id = key.id
        left join namespace ns on key.namespace_id = ns.id
        join project p on key.project_id = p.id
        join translation target on
              target.key_id = key.id and
              target.language_id = :targetLanguageId and
              target.text <> '' and
              target.text is not null
        where baseTranslation.language_id = p.base_language_id
          and (cast(:key as bigint) is null or key.id <> :key)
          and key.is_plural = :isPlural
          and baseTranslation.text % :baseTranslationText
        order by baseTranslation.text <-> :baseTranslationText
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
