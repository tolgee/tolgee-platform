package io.tolgee.service.queryBuilders.translationViewBuilder

import io.tolgee.constants.Feature
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.model.Project
import io.tolgee.model.enums.TranslationCommentState
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.TranslationSuggestionState
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.model.views.TranslationView
import io.tolgee.repository.qa.LanguageQaConfigRepository
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
  private val languageQaConfigRepository: LanguageQaConfigRepository,
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
    val qaDisabledLanguageIds = resolveQaDisabledLanguageIds(projectId, qaEnabled)
    val tasksEnabled = areTasksEnabled(project)

    createFailedKeysInJobTempTable(params.filterFailedKeysOfJob)

    val countBuilder =
      getTranslationsViewQueryBuilder(
        projectId,
        languages,
        params,
        pageable,
        cursor,
        qaEnabled,
        qaDisabledLanguageIds,
        tasksEnabled,
      )
    val count = em.createQuery(countBuilder.countQuery).singleResult

    val translationsViewQueryBuilder =
      getTranslationsViewQueryBuilder(
        projectId,
        languages,
        params,
        pageable,
        cursor,
        qaEnabled,
        qaDisabledLanguageIds,
        tasksEnabled,
      )
    val query = em.createQuery(translationsViewQueryBuilder.dataQuery).setMaxResults(pageable.pageSize)
    if (cursor == null) {
      query.firstResult = pageable.offset.toInt()
    }
    // The main query returns only key-level data. Translations are fetched separately below.
    val views = query.resultList.map { KeyWithTranslationsView.of(it, trashed = params.trashed) }

    deleteFailedKeysInJobTempTable()

    val keyIds = views.map { it.keyId }
    populateTranslationsWithCounts(keyIds, languages, views, qaEnabled, qaDisabledLanguageIds)

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
      populateQaIssues(translationIds, views, qaDisabledLanguageIds)
    }
    return PageImpl(views, pageable, count)
  }

  /**
   * Fetches all translations for the page's keys in the requested languages, together with
   * comment, suggestion and QA issue counts, in a **single query**. The inline count subqueries
   * are cheap because they only run on the page's translations (~pageSize × langCount rows,
   * typically <500), not on the entire project's translation set.
   *
   * Every `(key, language)` pair starts as a placeholder [TranslationView] in the `UNTRANSLATED`
   * state, so that every returned view has an entry for every requested language. The query only
   * returns the pairs carrying data: the full join adds the pairs that have suggestions but no
   * translation, and those only get their suggestion counts set on the placeholder.
   *
   * Both sides of the full join must filter on the page's keys and languages themselves. A filter
   * outside the join would have to let the unmatched rows through, which forces a scan of the whole
   * translation table. A row per `(key, language)` pair (`key × language left join translation`)
   * was measured up to 20× slower on projects with 100+ selected languages, where its cost estimate
   * crosses Postgres' `jit_above_cost`.
   *
   * The QA issue count expression ([qaCountExpr]) has three shapes: a literal `0L` when QA is off
   * project-wide, a plain count subquery when enabled, and a count subquery with a
   * `languageId not in (:qaDisabledLanguageIds)` exclusion when some languages have QA disabled.
   * The `0L` shape keeps the column layout stable.
   */
  @Suppress("LongMethod")
  private fun populateTranslationsWithCounts(
    keyIds: List<Long>,
    languages: Set<LanguageDto>,
    views: List<KeyWithTranslationsView>,
    qaEnabled: Boolean,
    qaDisabledLanguageIds: Set<Long>,
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

    val excludeDisabled = qaEnabled && qaDisabledLanguageIds.isNotEmpty()
    val qaCountExpr = qaCountExpr(qaEnabled, excludeDisabled)

    keyIds.chunked(IN_CLAUSE_CHUNK_SIZE).forEach { keyIdChunk ->
      val query =
        em
          .createQuery(
            """
            select t.id, t.text, t.state, t.auto, t.mtProvider, t.outdated, t.qaChecksStale,
                   coalesce(t.keyId, s.keyId), coalesce(t.languageId, s.languageId),
                   (select count(tc) from TranslationComment tc
                    where tc.translation.id = t.id),
                   (select sum(case when tc2.state = :unresolved then 1L else 0L end)
                    from TranslationComment tc2 where tc2.translation.id = t.id),
                   s.total,
                   s.active,
                   $qaCountExpr
            from (
              select tr.id as id, tr.text as text, tr.state as state, tr.auto as auto,
                     tr.mtProvider as mtProvider, tr.outdated as outdated,
                     tr.qaChecksStale as qaChecksStale,
                     tr.key.id as keyId, tr.language.id as languageId
              from Translation tr
              where tr.key.id in :keyIds and tr.language.id in :languageIds
            ) t
            full join (
              select ts.key.id as keyId, ts.language.id as languageId, count(ts) as total,
                     sum(case when ts.state = :activeSuggestion then 1L else 0L end) as active
              from TranslationSuggestion ts
              where ts.key.id in :keyIds and ts.language.id in :languageIds
              group by ts.key.id, ts.language.id
            ) s on s.keyId = t.keyId and s.languageId = t.languageId
            """.trimIndent(),
            Tuple::class.java,
          ).setParameter("keyIds", keyIdChunk)
          .setParameter("languageIds", languageIds)
          .setParameter("unresolved", TranslationCommentState.NEEDS_RESOLUTION)
          .setParameter("activeSuggestion", TranslationSuggestionState.ACTIVE)
      if (qaEnabled) {
        query.setParameter("openQa", QaIssueState.OPEN)
      }
      if (excludeDisabled) {
        query.setParameter("qaDisabledLanguageIds", qaDisabledLanguageIds)
      }

      for (row in query.resultList) {
        val keyId = row.get(7) as Long
        val langId = row.get(8) as Long
        val view = viewsByKeyId[keyId] ?: continue
        val tag = tagById[langId] ?: continue
        val translationId = row.get(0) as Long?
        if (translationId == null) {
          view.translations[tag]?.apply {
            activeSuggestionCount = (row.get(12) as Number?)?.toLong() ?: 0L
            totalSuggestionCount = (row.get(11) as Number?)?.toLong() ?: 0L
          }
          continue
        }
        view.translations[tag] =
          TranslationView(
            id = translationId,
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

  private fun areTasksEnabled(project: Project) =
    projectFeatureGuard.isFeatureEnabled(Feature.TASKS, project) ||
      projectFeatureGuard.isFeatureEnabled(Feature.ORDER_TRANSLATION, project)

  private fun resolveQaDisabledLanguageIds(
    projectId: Long,
    qaEnabled: Boolean,
  ): Set<Long> {
    if (!qaEnabled) return emptySet()
    return languageQaConfigRepository.findDisabledLanguageIds(projectId)
  }

  private fun qaCountExpr(
    qaEnabled: Boolean,
    excludeDisabled: Boolean,
  ): String {
    if (!qaEnabled) return "0L"
    if (!excludeDisabled) {
      return """(select count(qa) from TranslationQaIssue qa
            where qa.translation.id = t.id and qa.state = :openQa)"""
    }
    return """(select count(qa) from TranslationQaIssue qa
            where qa.translation.id = t.id and qa.state = :openQa
              and t.languageId not in :qaDisabledLanguageIds)"""
  }

  /**
   * Batch-loads the actual `TranslationQaIssue` objects for the page's translations and
   * attaches them to the matching `TranslationView.qaIssues` lists. Only called when the caller
   * opts in via `includeQaIssues = true` and the project has QA Checks enabled.
   */
  private fun populateQaIssues(
    translationIds: List<Long>,
    views: List<KeyWithTranslationsView>,
    qaDisabledLanguageIds: Set<Long>,
  ) {
    val qaIssuesMap =
      qaIssueRepository
        .findByTranslationIds(translationIds)
        .filterNot { it.translation.language.id in qaDisabledLanguageIds }
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
    val qaDisabledLanguageIds = resolveQaDisabledLanguageIds(projectId, qaEnabled)
    val tasksEnabled = areTasksEnabled(project)
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
        qaDisabledLanguageIds = qaDisabledLanguageIds,
        tasksEnabled = tasksEnabled,
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
    qaDisabledLanguageIds: Set<Long>,
    tasksEnabled: Boolean,
  ) = TranslationsViewQueryBuilder(
    cb = em.criteriaBuilder,
    projectId = projectId,
    languages = languages,
    params = params,
    sort = pageable.sort,
    cursor = cursor?.let { CursorUtil.parseCursor(it) },
    entityManager = em,
    qaEnabled = qaEnabled,
    qaDisabledLanguageIds = qaDisabledLanguageIds,
    tasksEnabled = tasksEnabled,
  )
}
