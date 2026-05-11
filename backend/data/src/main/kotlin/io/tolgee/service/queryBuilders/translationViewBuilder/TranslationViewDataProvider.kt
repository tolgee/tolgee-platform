package io.tolgee.service.queryBuilders.translationViewBuilder

import io.tolgee.constants.Feature
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.model.enums.TranslationCommentState
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.TranslationSuggestionState
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.model.views.TranslationView
import io.tolgee.repository.qa.TranslationQaIssueRepository
import io.tolgee.service.key.TagService
import io.tolgee.service.label.LabelService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.project.ProjectService
import io.tolgee.service.queryBuilders.CursorUtil
import jakarta.persistence.EntityManager
import jakarta.persistence.Tuple
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
  private val qaIssueRepository: TranslationQaIssueRepository,
  private val projectFeatureGuard: ProjectFeatureGuard,
  private val projectService: ProjectService,
) {
  companion object {
    private const val IN_CLAUSE_CHUNK_SIZE = 10_000
  }

  fun getData(
    projectId: Long,
    languages: Set<LanguageDto>,
    pageable: Pageable,
    params: TranslationFilters = TranslationFilters(),
    cursor: String? = null,
    includeQaIssues: Boolean = false,
  ): Page<KeyWithTranslationsView> {
    val project = projectService.get(projectId)
    val qaEnabled = projectFeatureGuard.isFeatureEnabled(Feature.QA_CHECKS, project)

    createFailedKeysInJobTempTable(params.filterFailedKeysOfJob)

    val countBuilder = getTranslationsViewQueryBuilder(projectId, languages, params, pageable, cursor, qaEnabled)
    val count = em.createQuery(countBuilder.countQuery).singleResult

    val translationsViewQueryBuilder =
      getTranslationsViewQueryBuilder(projectId, languages, params, pageable, cursor, qaEnabled)
    val query = em.createQuery(translationsViewQueryBuilder.dataQuery).setMaxResults(pageable.pageSize)
    if (cursor == null) {
      query.firstResult = pageable.offset.toInt()
    }
    // The main query returns only key-level data. Translations are fetched separately below.
    val views = query.resultList.map { KeyWithTranslationsView.of(it, trashed = params.trashed) }

    deleteFailedKeysInJobTempTable()

    val keyIds = views.map { it.keyId }
    // Fetch translations + all per-translation counts (comments, suggestions, QA issues)
    // in a single query. The inline count subqueries are cheap because they only run on
    // the page's translations (~pageSize × langCount rows, typically <500).
    populateTranslationsWithCounts(keyIds, languages, views, qaEnabled)

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
    if (qaEnabled && includeQaIssues && translationIds.isNotEmpty()) {
      populateQaIssues(translationIds, views)
    }
    return PageImpl(views, pageable, count)
  }

  /**
   * Fetches all translations for the page's keys in the requested languages, together with
   * comment, suggestion and QA issue counts, in a **single query**. The inline count subqueries
   * are cheap because they only run on the page's translations (~pageSize × langCount rows,
   * typically <500), not on the entire project's translation set.
   *
   * Missing `(key, language)` pairs are filled in with a placeholder [TranslationView] in the
   * `UNTRANSLATED` state so that every returned view has an entry for every requested language.
   *
   * This combines what would otherwise be 4 separate queries (translation fetch + 3 count
   * queries) into 1. The QA issue count subquery is only included when [qaEnabled] is true;
   * otherwise a literal `0L` is selected so the column layout (and the `COL_*` indices) stays
   * stable.
   *
   * Uses plain JPQL rather than Criteria API because the query has 5 inline correlated count
   * subqueries plus conditional logic (`qaCountExpr` toggles between a real subquery and `0L`).
   * Expressing that in Criteria API would require dozens of lines of nested `subquery.correlate()`
   * and conditional builders. The JPQL string is more readable for a fixed-shape query with no
   * dynamic predicates.
   */
  @Suppress("LongMethod")
  private fun populateTranslationsWithCounts(
    keyIds: List<Long>,
    languages: Set<LanguageDto>,
    views: List<KeyWithTranslationsView>,
    qaEnabled: Boolean,
  ) {
    if (languages.isEmpty()) return

    for (view in views) {
      for (language in languages) {
        view.translations[language.tag] = untranslatedPlaceholder()
      }
    }

    if (keyIds.isEmpty()) return

    val languageIds = languages.map { it.id }
    val viewsByKeyId: Map<Long, KeyWithTranslationsView> = views.associateBy { it.keyId }
    val tagById: Map<Long, String> = languages.associate { it.id to it.tag }

    // The QA issue count subquery is only included when QA checks are enabled for the project.
    // When disabled, a literal 0L keeps the column layout stable so the COL_* indices stay valid.
    val qaCountExpr =
      if (qaEnabled) {
        """(select count(qa) from TranslationQaIssue qa
            where qa.translation.id = t.id and qa.state = :openQa)"""
      } else {
        "0L"
      }

    keyIds.chunked(IN_CLAUSE_CHUNK_SIZE).forEach { keyIdChunk ->
      val query =
        em
          .createQuery(
            """
            select t.id, t.text, t.state, t.auto, t.mtProvider, t.outdated, t.qaChecksStale,
                   t.key.id, t.language.id,
                   (select count(tc) from TranslationComment tc
                    where tc.translation.id = t.id),
                   (select sum(case when tc2.state = :unresolved then 1L else 0L end)
                    from TranslationComment tc2 where tc2.translation.id = t.id),
                   (select count(ts) from TranslationSuggestion ts
                    where ts.key.id = t.key.id and ts.language.id = t.language.id),
                   (select sum(case when ts2.state = :activeSuggestion then 1L else 0L end)
                    from TranslationSuggestion ts2
                    where ts2.key.id = t.key.id and ts2.language.id = t.language.id),
                   $qaCountExpr
            from Translation t
            where t.key.id in :keyIds and t.language.id in :languageIds
            """.trimIndent(),
            Tuple::class.java,
          ).setParameter("keyIds", keyIdChunk)
          .setParameter("languageIds", languageIds)
          .setParameter("unresolved", TranslationCommentState.NEEDS_RESOLUTION)
          .setParameter("activeSuggestion", TranslationSuggestionState.ACTIVE)
      if (qaEnabled) {
        query.setParameter("openQa", QaIssueState.OPEN)
      }

      for (row in query.resultList) {
        val keyId = row.get(7) as Long
        val langId = row.get(8) as Long
        val view = viewsByKeyId[keyId] ?: continue
        val tag = tagById[langId] ?: continue
        view.translations[tag] =
          TranslationView(
            id = row.get(0) as Long?,
            text = row.get(1) as String?,
            state = row.get(2) as TranslationState,
            auto = row.get(3) as Boolean,
            mtProvider = row.get(4) as io.tolgee.constants.MtServiceType?,
            commentCount = (row.get(9) as Number?)?.toLong() ?: 0L,
            unresolvedCommentCount = (row.get(10) as Number?)?.toLong() ?: 0L,
            outdated = row.get(5) as Boolean,
            activeSuggestionCount = (row.get(12) as Number?)?.toLong() ?: 0L,
            totalSuggestionCount = (row.get(11) as Number?)?.toLong() ?: 0L,
            qaIssueCount = (row.get(13) as Number?)?.toLong() ?: 0L,
            qaChecksStale = (row.get(6) as Boolean?) ?: false,
          )
      }
    }
  }

  private fun untranslatedPlaceholder(): TranslationView =
    TranslationView(
      id = null,
      text = null,
      state = TranslationState.UNTRANSLATED,
      auto = false,
      mtProvider = null,
      commentCount = 0L,
      unresolvedCommentCount = 0L,
      outdated = false,
      activeSuggestionCount = 0L,
      totalSuggestionCount = 0L,
      qaIssueCount = 0L,
      qaChecksStale = false,
    )

  /**
   * Batch-loads the actual `TranslationQaIssue` objects for the page's translations and
   * attaches them to the matching `TranslationView.qaIssues` lists. Only called when the caller
   * opts in via `includeQaIssues = true` and the project has QA Checks enabled.
   */
  private fun populateQaIssues(
    translationIds: List<Long>,
    views: List<KeyWithTranslationsView>,
  ) {
    val qaIssuesMap =
      qaIssueRepository
        .findByTranslationIds(translationIds)
        .groupBy { it.translation.id }
    if (qaIssuesMap.isEmpty()) return
    views.forEach { view ->
      view.translations.values.forEach { translation ->
        translation.qaIssues = qaIssuesMap[translation.id] ?: emptyList()
      }
    }
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
    val project = projectService.get(projectId)
    val qaEnabled = projectFeatureGuard.isFeatureEnabled(Feature.QA_CHECKS, project)
    createFailedKeysInJobTempTable(params.filterFailedKeysOfJob)
    val translationsViewQueryBuilder =
      TranslationsViewQueryBuilder(
        cb = em.criteriaBuilder,
        projectId = projectId,
        languages = languages,
        params = params,
        sort = Sort.by(Sort.Order.asc(KeyWithTranslationsView::keyId.name)),
        entityManager = em,
        qaEnabled = qaEnabled,
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
    qaEnabled: Boolean,
  ) = TranslationsViewQueryBuilder(
    cb = em.criteriaBuilder,
    projectId = projectId,
    languages = languages,
    params = params,
    sort = pageable.sort,
    cursor = cursor?.let { CursorUtil.parseCursor(it) },
    entityManager = em,
    qaEnabled = qaEnabled,
  )
}
