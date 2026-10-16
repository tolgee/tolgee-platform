package io.tolgee.ee.service.aiReview

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.AiReviewRequest
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.ee.data.aiReview.AiReviewRunCounts
import io.tolgee.ee.data.aiReview.AiReviewRunRequest
import io.tolgee.ee.service.aiReview.AiReviewRunClassifier.Category
import io.tolgee.ee.service.aiReview.AiReviewRunClassifier.Kind
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.branching.Branch
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.repository.aiReview.AiReviewRuleRepository
import io.tolgee.service.language.LanguageService
import io.tolgee.util.aiReviewTextHash
import io.tolgee.util.aiReviewTextHashSql
import jakarta.persistence.EntityManager
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.PreparedStatement
import java.sql.ResultSet

@Service
class AiReviewRunService(
  private val aiReviewRuleRepository: AiReviewRuleRepository,
  private val triggerService: AiReviewTriggerService,
  private val languageService: LanguageService,
  private val batchJobService: BatchJobService,
  private val jdbcTemplate: JdbcTemplate,
  private val entityManager: EntityManager,
) {
  data class Preview(
    val total: Long,
    val withoutRereview: AiReviewRunCounts,
    val withRereview: AiReviewRunCounts,
  )

  private data class Scope(
    val projectId: Long,
    val keyIds: List<Long>?,
    val languageIds: List<Long>,
    val rules: List<AiReviewRule>,
    val qaCoveredLanguageIds: Set<Long>,
    val scopeRule: AiReviewRule?,
  ) {
    fun context(rereview: Boolean) =
      AiReviewRunClassifier.Context(
        rules = rules,
        qaCoveredLanguageIds = qaCoveredLanguageIds,
        rereview = rereview,
        scopeRule = scopeRule,
      )
  }

  private data class Group(
    val candidate: AiReviewRunClassifier.Candidate,
    val count: Long,
  )

  private data class Row(
    val translationId: Long,
    val keyId: Long,
    val text: String?,
    val candidate: AiReviewRunClassifier.Candidate,
  )

  @Transactional(readOnly = true)
  fun preview(
    project: ProjectDto,
    request: AiReviewRunRequest,
  ): Preview {
    val scope = resolveScope(project, request)
    val counted =
      findGroups(scope).filter {
        AiReviewRunClassifier.classify(it.candidate, scope.context(false)).kind != Kind.OUT_OF_SCOPE
      }
    return Preview(
      total = counted.sumOf { it.count },
      withoutRereview = count(counted, scope.context(false)),
      withRereview = count(counted, scope.context(true)),
    )
  }

  @Transactional(readOnly = true)
  fun getRuleBacklogs(
    project: ProjectDto,
    isLanguageRunnable: (languageId: Long) -> Boolean,
  ): Map<Long, Long> {
    val rules = aiReviewRuleRepository.findAllEnabledByProjectId(project.id)
    if (rules.isEmpty()) return emptyMap()
    val scope =
      Scope(
        projectId = project.id,
        keyIds = null,
        languageIds = getNonBaseLanguageIds(project.id).filter(isLanguageRunnable),
        rules = rules,
        qaCoveredLanguageIds = triggerService.getQaCoveredLanguageIds(project),
        scopeRule = null,
      )
    val notQueuedOrFailed = findGroups(scope).filter { it.candidate.category !in NOT_IN_BACKLOG }
    return rules.associate { rule ->
      val context = scope.copy(scopeRule = rule).context(false)
      rule.id to
        notQueuedOrFailed
          .filter { AiReviewRunClassifier.classify(it.candidate, context).kind == Kind.REVIEW }
          .sumOf { it.count }
    }
  }

  @Transactional
  fun start(
    project: ProjectDto,
    request: AiReviewRunRequest,
    author: UserAccount,
  ): BatchJob {
    val scope = resolveScope(project, request)
    val context = scope.context(request.rereview)
    val keyIds = scope.keyIds ?: findDefaultBranchKeyIds(project.id)
    val targets = mutableListOf<BatchTranslationTargetItem>()
    val toQueue = mutableListOf<AiReviewTriggerService.RunTarget>()
    if (scope.languageIds.isNotEmpty()) {
      keyIds.chunked(KEY_CHUNK_SIZE).forEach { chunk ->
        findRows(scope.copy(keyIds = chunk)).forEach { row ->
          val decision = AiReviewRunClassifier.classify(row.candidate, context)
          if (decision.kind != Kind.REVIEW) return@forEach
          targets.add(BatchTranslationTargetItem(row.keyId, row.candidate.languageId))
          if (row.candidate.category == Category.PENDING) return@forEach
          toQueue.add(
            AiReviewTriggerService.RunTarget(
              translationId = row.translationId,
              textHash = aiReviewTextHash(row.text),
              rereviewOfAutoReviewed = decision.rereviewOfAutoReviewed,
            ),
          )
        }
      }
    }
    if (targets.isEmpty()) throw BadRequestException(Message.AI_REVIEW_NOTHING_TO_REVIEW)
    val job =
      batchJobService.startJob(
        request = AiReviewRequest(target = targets),
        project = entityManager.getReference(Project::class.java, project.id),
        author = author,
        type = BatchJobType.AI_REVIEW,
      )
    triggerService.upsertPendingForRun(toQueue, runBatchJobId = job.id)
    return job
  }

  fun getCandidateLanguageIds(
    project: ProjectDto,
    request: AiReviewRunRequest,
  ): List<Long> = resolveLanguageIds(project.id, request.languageIds, findScopeRule(project, request))

  private fun resolveScope(
    project: ProjectDto,
    request: AiReviewRunRequest,
  ): Scope {
    if (request.keyIds == null && request.ruleId == null) {
      throw BadRequestException(Message.AI_REVIEW_RUN_SCOPE_REQUIRED)
    }
    val rules = aiReviewRuleRepository.findAllEnabledByProjectId(project.id)
    val scopeRule = findScopeRule(project, request)
    return Scope(
      projectId = project.id,
      keyIds = request.keyIds?.distinct(),
      languageIds = resolveLanguageIds(project.id, request.languageIds, scopeRule),
      rules = rules,
      qaCoveredLanguageIds = triggerService.getQaCoveredLanguageIds(project),
      scopeRule = scopeRule,
    )
  }

  private fun findScopeRule(
    project: ProjectDto,
    request: AiReviewRunRequest,
  ): AiReviewRule? {
    val ruleId = request.ruleId ?: return null
    return aiReviewRuleRepository.findByProjectIdAndId(project.id, ruleId)
      ?: throw NotFoundException(Message.AI_REVIEW_RULE_NOT_FOUND)
  }

  private fun resolveLanguageIds(
    projectId: Long,
    requested: List<Long>?,
    scopeRule: AiReviewRule?,
  ): List<Long> {
    val nonBase = getNonBaseLanguageIds(projectId)
    val ruleLanguageIds = scopeRule?.languageIds?.takeIf { it.isNotEmpty() }
    val allowed = ruleLanguageIds?.let { ids -> nonBase.filter { it in ids } } ?: nonBase
    if (requested == null) return allowed
    return requested.distinct().filter { it in allowed }
  }

  private fun getNonBaseLanguageIds(projectId: Long): List<Long> {
    val baseLanguageId = languageService.getProjectBaseLanguage(projectId).id
    return languageService.getProjectLanguages(projectId).map { it.id }.filter { it != baseLanguageId }
  }

  private fun count(
    groups: List<Group>,
    context: AiReviewRunClassifier.Context,
  ): AiReviewRunCounts {
    val byKind = mutableMapOf<Kind, Long>()
    var dryRunOnly = 0L
    groups.forEach { group ->
      val decision = AiReviewRunClassifier.classify(group.candidate, context)
      byKind.merge(decision.kind, group.count, Long::plus)
      if (decision.kind != Kind.REVIEW) return@forEach
      if (checkNotNull(decision.rule).mode == AiReviewRuleMode.DRY_RUN) dryRunOnly += group.count
    }
    return AiReviewRunCounts(
      willReview = byKind[Kind.REVIEW] ?: 0,
      dryRunOnly = dryRunOnly,
      alreadyHasResult = byKind[Kind.ALREADY_HAS_RESULT] ?: 0,
      humanReviewed = byKind[Kind.HUMAN_REVIEWED] ?: 0,
      noRule = byKind[Kind.NO_RULE] ?: 0,
      never = byKind[Kind.NEVER] ?: 0,
      openQaIssues = byKind[Kind.OPEN_QA_ISSUES] ?: 0,
    )
  }

  private fun findGroups(scope: Scope): List<Group> {
    if (scope.languageIds.isEmpty()) return emptyList()
    val sql =
      """
      SELECT ns.name AS namespace, $TAGS_SQL AS tags, t.language_id, $CATEGORY_SQL AS category,
        $OPEN_QA_SQL AS has_open_qa, $BRANCH_SQL AS branch, count(*) AS cnt
      ${fromSql(scope)}
      GROUP BY 1, 2, 3, 4, 5, 6
      """.trimIndent()
    return jdbcTemplate.query(sql, { ps -> setScopeParams(ps, scope) }) { rs, _ ->
      Group(
        candidate = rs.toCandidate(),
        count = rs.getLong("cnt"),
      )
    }
  }

  private fun findDefaultBranchKeyIds(projectId: Long): List<Long> =
    jdbcTemplate
      .queryForList(
        """
        SELECT k.id FROM key k
        LEFT JOIN branch br ON br.id = k.branch_id
        WHERE k.project_id = ? AND k.deleted_at IS NULL AND (br.id IS NULL OR br.is_default)
        ORDER BY k.id
        """.trimIndent(),
        Long::class.java,
        projectId,
      ).filterNotNull()

  private fun findRows(scope: Scope): List<Row> {
    val sql =
      """
      SELECT t.id, t.key_id, t.text, ns.name AS namespace, $TAGS_SQL AS tags, t.language_id,
        $CATEGORY_SQL AS category, $OPEN_QA_SQL AS has_open_qa, $BRANCH_SQL AS branch
      ${fromSql(scope)}
      """.trimIndent()
    return jdbcTemplate.query(sql, { ps -> setScopeParams(ps, scope) }) { rs, _ ->
      Row(
        translationId = rs.getLong("id"),
        keyId = rs.getLong("key_id"),
        text = rs.getString("text"),
        candidate = rs.toCandidate(),
      )
    }
  }

  private fun fromSql(scope: Scope): String {
    val keyCondition = getKeyCondition(scope)
    return """
      FROM translation t
      JOIN key k ON k.id = t.key_id
      LEFT JOIN namespace ns ON ns.id = k.namespace_id
      LEFT JOIN branch br ON br.id = k.branch_id
      LEFT JOIN translation_ai_review r ON r.translation_id = t.id AND r.text_hash = ${aiReviewTextHashSql("t.text")}
      WHERE k.project_id = ? AND k.deleted_at IS NULL
        AND t.language_id = ANY(?)
        AND t.text IS NOT NULL AND btrim(t.text) <> ''
        AND t.state IN (${TranslationState.TRANSLATED.ordinal}, ${TranslationState.REVIEWED.ordinal})
        AND $keyCondition
      """
  }

  private fun getKeyCondition(scope: Scope): String {
    if (scope.keyIds != null) return "k.id = ANY(?)"
    return "(br.id IS NULL OR br.is_default)"
  }

  private fun setScopeParams(
    ps: PreparedStatement,
    scope: Scope,
  ) {
    ps.setLong(1, scope.projectId)
    ps.setArray(2, ps.connection.createArrayOf("bigint", scope.languageIds.toTypedArray()))
    scope.keyIds?.let { ps.setArray(3, ps.connection.createArrayOf("bigint", it.toTypedArray())) }
  }

  private fun ResultSet.toCandidate(): AiReviewRunClassifier.Candidate {
    @Suppress("UNCHECKED_CAST")
    val tags = (getArray("tags")?.array as? Array<String>)?.toSet() ?: emptySet()
    return AiReviewRunClassifier.Candidate(
      category = Category.valueOf(getString("category")),
      hasOpenQaIssues = getBoolean("has_open_qa"),
      scope =
        AiReviewRuleMatcher.KeyScope(
          tags = tags,
          namespace = getString("namespace"),
          branch = getString("branch") ?: Branch.DEFAULT_BRANCH_NAME,
        ),
      languageId = getLong("language_id"),
    )
  }

  companion object {
    private const val KEY_CHUNK_SIZE = 10_000
    private val NOT_IN_BACKLOG = setOf(Category.PENDING, Category.FAILED)

    private const val BRANCH_SQL =
      "coalesce(br.name, (SELECT db.name FROM branch db WHERE db.project_id = k.project_id AND db.is_default))"

    private const val TAGS_SQL =
      """ARRAY(
        SELECT tg.name FROM key_meta km
        JOIN key_meta_tags kmt ON kmt.key_metas_id = km.id
        JOIN tag tg ON tg.id = kmt.tags_id
        WHERE km.key_id = k.id ORDER BY tg.name
      )"""

    private val CATEGORY_SQL =
      """CASE
        WHEN t.state = ${TranslationState.REVIEWED.ordinal} AND r.outcome = 'AUTO_REVIEWED' THEN 'AUTO_REVIEWED'
        WHEN t.state = ${TranslationState.REVIEWED.ordinal} AND r.outcome = 'PENDING'
          AND r.previous_outcome = 'AUTO_REVIEWED'
          THEN 'PENDING'
        WHEN t.state = ${TranslationState.REVIEWED.ordinal} THEN 'HUMAN_REVIEWED'
        WHEN r.outcome = 'REJECTED' THEN 'HUMAN_REVIEWED'
        WHEN r.outcome = 'PENDING' THEN 'PENDING'
        WHEN r.outcome = 'FAILED' THEN 'FAILED'
        WHEN r.outcome IS NULL THEN 'NO_RESULT'
        ELSE 'HAS_RESULT'
      END"""

    private const val OPEN_QA_SQL =
      """EXISTS (SELECT 1 FROM translation_qa_issue q WHERE q.translation_id = t.id AND q.state = 'OPEN')"""
  }
}
