package io.tolgee.service.queryBuilders.translationViewBuilder

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.key.TagService
import io.tolgee.service.label.LabelService
import io.tolgee.service.queryBuilders.CursorUtil
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component

@Component
class TranslationViewDataProvider(
  private val em: EntityManager,
  private val tagService: TagService,
  private val labelService: LabelService,
  private val authenticationFacade: AuthenticationFacade,
) {
  fun getData(
    projectId: Long,
    languages: Set<LanguageDto>,
    pageable: Pageable,
    params: TranslationFilters = TranslationFilters(),
    cursor: String? = null,
  ): Page<KeyWithTranslationsView> {
    // otherwise it takes forever for postgres to plan the execution
    em.createNativeQuery("SET join_collapse_limit TO 1").executeUpdate()

    createFailedKeysInJobTempTable(params.filterFailedKeysOfJob)

    var translationsViewQueryBuilder = getTranslationsViewQueryBuilder(projectId, languages, params, pageable, cursor)
    val count = em.createQuery(translationsViewQueryBuilder.countQuery).singleResult

    translationsViewQueryBuilder = getTranslationsViewQueryBuilder(projectId, languages, params, pageable, cursor)
    val query = em.createQuery(translationsViewQueryBuilder.dataQuery).setMaxResults(pageable.pageSize)
    if (cursor == null) {
      query.firstResult = pageable.offset.toInt()
    }
    val views = query.resultList.map { KeyWithTranslationsView.of(it, languages.toList()) }

    // reset the value
    em.createNativeQuery("SET join_collapse_limit TO DEFAULT").executeUpdate()
    deleteFailedKeysInJobTempTable()

    val keyIds = views.map { it.keyId }
    val translationIds =
      views
        .flatMap { it.translations.values }
        .filter { it.id != null }
        .map { it.id!! }
    tagService.getTagsForKeyIds(keyIds).let { tagMap ->
      views.forEach { it.keyTags = tagMap[it.keyId] ?: emptyList() }
    }
    labelService.getByTranslationIdsIndexed(translationIds).let { labels ->
      views.forEach { view ->
        view.translations.values.forEach { translation ->
          translation.labels = labels[translation.id] ?: listOf()
        }
      }
    }
    return PageImpl(views, pageable, count)
  }

  private fun createFailedKeysInJobTempTable(filterFailedKeysOfJob: Long?) {
    if (filterFailedKeysOfJob == null) {
      return
    }

    em
      .createNativeQuery(
        """
        CREATE TEMP TABLE temp_unsuccessful_job_keys AS
            WITH unsuccessful_targets AS (
                SELECT *
                FROM (
                         SELECT jsonb_array_elements(bj.target) AS target
                         FROM tolgee_batch_job bj
                         WHERE bj.id = :batchJobId
                     ) AS targets
                WHERE targets.target NOT IN (
                    SELECT jsonb_array_elements(tbje.success_targets)
                    FROM tolgee_batch_job_chunk_execution tbje
                    WHERE tbje.batch_job_id = :batchJobId
                  )
            )
            SELECT DISTINCT (target -> 'keyId')\:\:bigint AS key_id
            FROM unsuccessful_targets;
      """,
      ).setParameter("batchJobId", filterFailedKeysOfJob)
      .executeUpdate()
  }

  private fun deleteFailedKeysInJobTempTable() {
    em
      .createNativeQuery("DROP TABLE IF EXISTS temp_unsuccessful_job_keys")
      .executeUpdate()
  }

  fun getSelectAllKeys(
    projectId: Long,
    languages: Set<LanguageDto>,
    params: TranslationFilters = TranslationFilters(),
  ): MutableList<Long> {
    createFailedKeysInJobTempTable(params.filterFailedKeysOfJob)
    val translationsViewQueryBuilder =
      TranslationsViewQueryBuilder(
        cb = em.criteriaBuilder,
        projectId = projectId,
        languages = languages,
        params = params,
        sort = Sort.by(Sort.Order.asc(KeyWithTranslationsView::keyId.name)),
        entityManager = em,
        authenticationFacade = authenticationFacade,
      )
    val result = em.createQuery(translationsViewQueryBuilder.keyIdsQuery).resultList
    deleteFailedKeysInJobTempTable()
    return result
  }

  private fun getTranslationsViewQueryBuilder(
    projectId: Long,
    languages: Set<LanguageDto>,
    params: TranslationFilters,
    pageable: Pageable,
    cursor: String?,
  ) = TranslationsViewQueryBuilder(
    cb = em.criteriaBuilder,
    projectId = projectId,
    languages = languages,
    params = params,
    sort = pageable.sort,
    cursor = cursor?.let { CursorUtil.parseCursor(it) },
    entityManager = em,
    authenticationFacade = authenticationFacade,
  )
}
