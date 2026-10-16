package io.tolgee.ee.service.aiReview

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.AiReviewRequest
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Feature
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.queryResults.aiReview.AiReviewTargetView
import io.tolgee.ee.service.qa.ProjectQaConfigService
import io.tolgee.model.ALLOCATION_SIZE
import io.tolgee.model.Project
import io.tolgee.model.SEQUENCE_NAME
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobStatus
import io.tolgee.model.branching.Branch
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.repository.KeyRepository
import io.tolgee.repository.TranslationRepository
import io.tolgee.repository.aiReview.AiReviewRuleRepository
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.util.SequenceIdProvider
import io.tolgee.util.aiReviewTextHash
import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Lazy
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.sql.Timestamp

@Service
class AiReviewTriggerService(
  private val translationAiReviewRepository: TranslationAiReviewRepository,
  private val aiReviewRuleRepository: AiReviewRuleRepository,
  private val keyRepository: KeyRepository,
  private val translationRepository: TranslationRepository,
  private val batchJobService: BatchJobService,
  private val entityManager: EntityManager,
  private val jdbcTemplate: JdbcTemplate,
  private val currentDateProvider: CurrentDateProvider,
  private val languageService: LanguageService,
  private val projectFeatureGuard: ProjectFeatureGuard,
  @Lazy private val projectQaConfigService: ProjectQaConfigService,
  private val objectMapper: ObjectMapper,
) {
  data class TextChange(
    val translationId: Long,
    val keyId: Long,
    val languageId: Long,
    val newText: String?,
  )

  fun invalidate(translationIds: Collection<Long>) {
    translationIds.distinct().chunked(IN_CLAUSE_CHUNK_SIZE).forEach {
      translationAiReviewRepository.deleteAllByTranslationIdIn(it)
    }
  }

  fun markRejectedByStateChange(
    translationIds: Collection<Long>,
    authorId: Long?,
  ) {
    translationIds.distinct().chunked(IN_CLAUSE_CHUNK_SIZE).forEach {
      translationAiReviewRepository.markAutoReviewedRejected(it, authorId, currentDateProvider.date)
    }
  }

  fun resetSiblingsOfChangedSources(
    projectId: Long,
    changes: List<TextChange>,
  ): List<TextChange> {
    if (changes.isEmpty()) return emptyList()
    val baseLanguageId = languageService.getProjectBaseLanguage(projectId).id
    val sourceKeyIds = changes.filter { it.languageId == baseLanguageId }.map { it.keyId }.distinct()
    if (sourceKeyIds.isEmpty()) return emptyList()
    val changedTranslationIds = changes.map { it.translationId }.toSet()
    val siblings =
      sourceKeyIds
        .chunked(IN_CLAUSE_CHUNK_SIZE)
        .flatMap {
          translationAiReviewRepository.findSiblingsToResetAfterSourceChange(
            it,
            baseLanguageId,
            RESET_BY_SOURCE_CHANGE_OUTCOMES,
          )
        }.filter { it.translationId !in changedTranslationIds }
    invalidate(siblings.map { it.translationId })
    return siblings.map {
      TextChange(translationId = it.translationId, keyId = it.keyId, languageId = it.languageId, newText = it.text)
    }
  }

  fun createPending(
    projectId: Long,
    changes: List<TextChange>,
  ): List<BatchTranslationTargetItem> {
    val baseLanguageId = languageService.getProjectBaseLanguage(projectId).id
    val candidates = changes.filter { it.languageId != baseLanguageId && !it.newText.isNullOrBlank() }
    if (candidates.isEmpty()) return emptyList()

    val rules = aiReviewRuleRepository.findAllEnabledByProjectId(projectId)
    if (rules.isEmpty()) return emptyList()

    val translatedIds = getTranslatedTranslationIds(candidates.map { it.translationId })
    val scopes = getKeyScopes(candidates.map { it.keyId })
    val eligible =
      candidates.filter { change ->
        if (change.translationId !in translatedIds) return@filter false
        val scope = scopes[change.keyId] ?: return@filter false
        AiReviewRuleMatcher.match(rules, scope, change.languageId) is AiReviewRuleMatcher.Match.Review
      }
    upsertPending(eligible)
    return eligible.map { BatchTranslationTargetItem(keyId = it.keyId, languageId = it.languageId) }.distinct()
  }

  fun startReviewIfNotQaCovered(
    projectId: Long,
    project: ProjectDto,
    targets: List<BatchTranslationTargetItem>,
  ) {
    if (targets.isEmpty()) return
    val qaLanguageIds = getQaCoveredLanguageIds(project)
    startJobs(projectId, targets.filter { it.languageId !in qaLanguageIds }, reevaluateOnly = false)
  }

  fun startReview(
    projectId: Long,
    targets: List<BatchTranslationTargetItem>,
  ) {
    startJobs(projectId, targets, reevaluateOnly = false)
  }

  fun startReviewAfterQa(
    projectId: Long,
    qaTargets: List<BatchTranslationTargetItem>,
  ) {
    if (qaTargets.isEmpty()) return
    val wantedPairs = qaTargets.map { it.keyId to it.languageId }.toSet()
    val pending =
      qaTargets
        .map { it.keyId }
        .distinct()
        .chunked(IN_CLAUSE_CHUNK_SIZE)
        .flatMap { keyIds ->
          translationAiReviewRepository.findPendingTargets(
            keyIds = keyIds,
            languageIds = qaTargets.map { it.languageId }.distinct(),
          )
        }.filter { !it.qaChecksStale && (it.keyId to it.languageId) in wantedPairs }
    startJobs(projectId, pending.toTargets(), reevaluateOnly = false)
  }

  fun startReviewForPendingKeys(
    projectId: Long,
    project: ProjectDto,
    keyIds: Collection<Long>,
  ) {
    if (keyIds.isEmpty()) return
    val pending =
      keyIds
        .distinct()
        .chunked(IN_CLAUSE_CHUNK_SIZE)
        .flatMap { translationAiReviewRepository.findPendingTargetsByKeyIds(it) }
    startReviewIfNotQaCovered(projectId, project, pending.toTargets())
  }

  fun startStuckPendingReviews(
    projectId: Long,
    project: ProjectDto,
  ) {
    if (batchJobService.hasActiveJobForProject(projectId, BatchJobType.AI_REVIEW)) return
    if (batchJobService.hasActiveJobForProject(projectId, BatchJobType.QA_CHECK)) return
    val qaLanguageIds = getQaCoveredLanguageIds(project)
    val stuck =
      translationAiReviewRepository
        .findPendingTargetsByProjectId(projectId)
        .filter { it.languageId !in qaLanguageIds || !it.qaChecksStale }
    startJobs(projectId, stuck.toTargets(), reevaluateOnly = false, handlingStuckItems = true)
  }

  fun startReevaluation(
    projectId: Long,
    keyIds: Collection<Long>,
  ) {
    if (keyIds.isEmpty()) return
    val targets =
      keyIds
        .distinct()
        .chunked(IN_CLAUSE_CHUNK_SIZE)
        .flatMap {
          translationAiReviewRepository.findTargetsByKeyIdsAndOutcomes(it, AiReviewOutcome.REEVALUATED)
        }
    startJobs(projectId, targets.toTargets(), reevaluateOnly = true)
  }

  fun startReevaluationAfterRuleChange(
    projectId: Long,
    rule: AiReviewRule,
  ) {
    if (!rule.enabled) return
    val reviewed =
      translationAiReviewRepository.findTargetsByProjectIdAndOutcomes(
        projectId,
        AiReviewOutcome.REEVALUATED,
      )
    if (reviewed.isEmpty()) return
    val scopes = getKeyScopes(reviewed.map { it.keyId })
    val matchedByRule =
      reviewed.filter { target ->
        val scope = scopes[target.keyId] ?: return@filter false
        AiReviewRuleMatcher.matches(rule, scope, target.languageId)
      }
    val storedForRule =
      translationAiReviewRepository.findTargetsByRuleIdAndOutcomes(
        rule.id,
        AiReviewOutcome.REEVALUATED,
      )
    startJobs(projectId, (matchedByRule + storedForRule).toTargets(), reevaluateOnly = true)
  }

  fun retryFailed(
    projectId: Long,
    ruleId: Long,
  ): Int {
    val changes =
      translationAiReviewRepository.findRetryableFailedByRuleId(ruleId).map {
        TextChange(translationId = it.translationId, keyId = it.keyId, languageId = it.languageId, newText = it.text)
      }
    if (changes.isEmpty()) return 0
    val targets = createPending(projectId, changes)
    val queued = targets.map { it.keyId to it.languageId }.toSet()
    invalidate(changes.filter { (it.keyId to it.languageId) !in queued }.map { it.translationId })
    startReview(projectId, targets)
    return targets.size
  }

  fun isQaCovered(
    project: ProjectDto,
    languageId: Long,
  ): Boolean = languageId in getQaCoveredLanguageIds(project)

  fun getKeyScopes(keyIds: Collection<Long>): Map<Long, AiReviewRuleMatcher.KeyScope> {
    if (keyIds.isEmpty()) return emptyMap()
    return keyIds
      .distinct()
      .chunked(IN_CLAUSE_CHUNK_SIZE)
      .flatMap { keyRepository.getNamespaceAndTagRows(it) }
      .groupBy { it.keyId }
      .mapValues { (_, keyRows) ->
        AiReviewRuleMatcher.KeyScope(
          tags = keyRows.mapNotNull { it.tag }.toSet(),
          namespace = keyRows.first().namespace,
          branch = keyRows.first().branch ?: Branch.DEFAULT_BRANCH_NAME,
        )
      }
  }

  fun getQaCoveredLanguageIds(project: ProjectDto): Set<Long> {
    if (!projectFeatureGuard.isFeatureEnabled(Feature.QA_CHECKS, project)) return emptySet()
    return projectQaConfigService.getEnabledLanguageIds(project.id)
  }

  data class RunTarget(
    val translationId: Long,
    val textHash: String,
    val rereviewOfAutoReviewed: Boolean,
  )

  fun upsertPendingForRun(
    targets: List<RunTarget>,
    runBatchJobId: Long,
  ) {
    if (targets.isEmpty()) return
    val sequenceIdProvider = SequenceIdProvider(SEQUENCE_NAME, ALLOCATION_SIZE)
    val now = Timestamp(currentDateProvider.date.time)
    jdbcTemplate.batchUpdate(
      """
      INSERT INTO translation_ai_review
        (id, created_at, updated_at, translation_id, outcome, text_hash, dry_run, run_batch_job_id)
      VALUES (?, ?, ?, ?, 'PENDING', ?, false, ?)
      ON CONFLICT (translation_id) DO UPDATE SET
        outcome = 'PENDING', text_hash = EXCLUDED.text_hash, claimed_at = NULL,
        run_batch_job_id = EXCLUDED.run_batch_job_id, updated_at = EXCLUDED.updated_at,
        previous_outcome = CASE WHEN $SAME_TEXT THEN translation_ai_review.outcome END,
        dry_run = $SAME_TEXT AND translation_ai_review.dry_run,
        ${RESULT_COLUMNS.joinToString(
        ",\n        ",
      ) { "$it = CASE WHEN $SAME_TEXT THEN translation_ai_review.$it END" }}
      WHERE (translation_ai_review.outcome = 'AUTO_REVIEWED' AND ?)
        OR translation_ai_review.outcome NOT IN ('AUTO_REVIEWED', 'PENDING', 'REJECTED')
        OR (translation_ai_review.outcome = 'PENDING' AND NOT $SAME_TEXT)
      """.trimIndent(),
      targets.distinctBy { it.translationId },
      BATCH_SIZE,
    ) { ps, target ->
      ps.setLong(1, sequenceIdProvider.next(ps.connection))
      ps.setTimestamp(2, now)
      ps.setTimestamp(3, now)
      ps.setLong(4, target.translationId)
      ps.setString(5, target.textHash)
      ps.setLong(6, runBatchJobId)
      ps.setBoolean(7, target.rereviewOfAutoReviewed)
    }
  }

  fun restoreOrDropPending(reviewIds: Collection<Long>) {
    reviewIds.distinct().chunked(IN_CLAUSE_CHUNK_SIZE).forEach { ids ->
      val idArray = ids.toTypedArray()
      jdbcTemplate.update(
        """
        UPDATE translation_ai_review
        SET outcome = previous_outcome, previous_outcome = NULL, run_batch_job_id = NULL, claimed_at = NULL
        WHERE id = ANY(?) AND outcome = 'PENDING' AND previous_outcome IS NOT NULL
        """.trimIndent(),
      ) { ps -> ps.setArray(1, ps.connection.createArrayOf("bigint", idArray)) }
      jdbcTemplate.update(
        "DELETE FROM translation_ai_review WHERE id = ANY(?) AND outcome = 'PENDING'",
      ) { ps -> ps.setArray(1, ps.connection.createArrayOf("bigint", idArray)) }
    }
  }

  fun abandonPendingOfCancelledRun(
    projectId: Long,
    runBatchJobId: Long,
  ) {
    val queued =
      jdbcTemplate.query(
        """
        SELECT r.id, t.key_id, t.language_id FROM translation_ai_review r
        JOIN translation t ON t.id = r.translation_id
        WHERE r.run_batch_job_id = ? AND r.outcome = 'PENDING' AND r.claimed_at IS NULL
        """.trimIndent(),
        { rs, _ ->
          rs.getLong("id") to BatchTranslationTargetItem(rs.getLong("key_id"), rs.getLong("language_id"))
        },
        runBatchJobId,
      )
    if (queued.isEmpty()) return
    val otherRunByTarget = getOtherActiveRunByTarget(projectId, runBatchJobId)
    val (handedOver, abandoned) = queued.partition { (_, target) -> target in otherRunByTarget }
    handedOver
      .groupBy({ (_, target) -> otherRunByTarget.getValue(target) }, { (id, _) -> id })
      .forEach { (otherRunId, ids) -> handOverToRun(ids, otherRunId) }
    restoreOrDropPending(abandoned.map { (id, _) -> id })
  }

  private fun getOtherActiveRunByTarget(
    projectId: Long,
    excludedRunId: Long,
  ): Map<BatchTranslationTargetItem, Long> {
    val runs =
      entityManager
        .createQuery(
          """
          from BatchJob j
          where j.project.id = :projectId and j.type = :type and j.hidden = false
            and j.status not in :completedStatuses and j.id <> :excludedRunId
          """.trimIndent(),
          BatchJob::class.java,
        ).setParameter("projectId", projectId)
        .setParameter("type", BatchJobType.AI_REVIEW)
        .setParameter("completedStatuses", BatchJobStatus.entries.filter { it.completed })
        .setParameter("excludedRunId", excludedRunId)
        .resultList
    return runs
      .flatMap { run ->
        run.target.map { objectMapper.convertValue(it, BatchTranslationTargetItem::class.java) to run.id }
      }.toMap()
  }

  private fun handOverToRun(
    reviewIds: List<Long>,
    runBatchJobId: Long,
  ) {
    reviewIds.chunked(IN_CLAUSE_CHUNK_SIZE).forEach { ids ->
      jdbcTemplate.update("UPDATE translation_ai_review SET run_batch_job_id = ? WHERE id = ANY(?)") { ps ->
        ps.setLong(1, runBatchJobId)
        ps.setArray(2, ps.connection.createArrayOf("bigint", ids.toTypedArray()))
      }
    }
  }

  private fun getTranslatedTranslationIds(translationIds: List<Long>): Set<Long> =
    translationIds
      .distinct()
      .chunked(IN_CLAUSE_CHUNK_SIZE)
      .flatMap { translationRepository.findIdsByIdInAndState(it, TranslationState.TRANSLATED) }
      .toSet()

  private fun upsertPending(changes: List<TextChange>) {
    if (changes.isEmpty()) return
    val sequenceIdProvider = SequenceIdProvider(SEQUENCE_NAME, ALLOCATION_SIZE)
    val now = Timestamp(currentDateProvider.date.time)
    jdbcTemplate.batchUpdate(
      """
      INSERT INTO translation_ai_review (id, created_at, updated_at, translation_id, outcome, text_hash, dry_run)
      VALUES (?, ?, ?, ?, 'PENDING', ?, false)
      ON CONFLICT (translation_id) DO UPDATE SET
        outcome = 'PENDING', text_hash = EXCLUDED.text_hash, rule_id = NULL, prompt_id = NULL,
        provider_name = NULL, score = NULL, threshold = NULL, dry_run = false, issues = NULL,
        error_code = NULL, error_detail = NULL, reviewed_at = NULL, resolved_by_id = NULL,
        resolved_at = NULL, claimed_at = NULL, previous_outcome = NULL,
        run_batch_job_id = NULL, updated_at = EXCLUDED.updated_at
      """.trimIndent(),
      changes.distinctBy { it.translationId },
      BATCH_SIZE,
    ) { ps, change ->
      ps.setLong(1, sequenceIdProvider.next(ps.connection))
      ps.setTimestamp(2, now)
      ps.setTimestamp(3, now)
      ps.setLong(4, change.translationId)
      ps.setString(5, aiReviewTextHash(change.newText))
    }
  }

  private fun startJobs(
    projectId: Long,
    targets: List<BatchTranslationTargetItem>,
    reevaluateOnly: Boolean,
    handlingStuckItems: Boolean = false,
  ) {
    if (targets.isEmpty()) return
    val project = entityManager.getReference(Project::class.java, projectId)
    targets.distinct().chunked(MAX_BATCH_JOB_TARGET_SIZE).forEach { chunk ->
      batchJobService.startJob(
        request =
          AiReviewRequest(
            target = chunk,
            reevaluateOnly = reevaluateOnly,
            handlingStuckItems = handlingStuckItems,
          ),
        project = project,
        author = null,
        type = BatchJobType.AI_REVIEW,
        isHidden = true,
      )
    }
  }

  private fun List<AiReviewTargetView>.toTargets(): List<BatchTranslationTargetItem> =
    map { BatchTranslationTargetItem(keyId = it.keyId, languageId = it.languageId) }

  companion object {
    private const val IN_CLAUSE_CHUNK_SIZE = 10_000
    private const val BATCH_SIZE = 1000
    private const val MAX_BATCH_JOB_TARGET_SIZE = 32_000
    private const val SAME_TEXT = "translation_ai_review.text_hash = EXCLUDED.text_hash"
    private val RESULT_COLUMNS =
      listOf(
        "rule_id",
        "prompt_id",
        "provider_name",
        "score",
        "threshold",
        "issues",
        "error_code",
        "error_detail",
        "reviewed_at",
        "resolved_by_id",
        "resolved_at",
      )
    private val RESET_BY_SOURCE_CHANGE_OUTCOMES =
      listOf(
        AiReviewOutcome.PENDING,
        AiReviewOutcome.WOULD_AUTO_REVIEW,
        AiReviewOutcome.BELOW_THRESHOLD,
        AiReviewOutcome.FAILED,
      )
  }
}
