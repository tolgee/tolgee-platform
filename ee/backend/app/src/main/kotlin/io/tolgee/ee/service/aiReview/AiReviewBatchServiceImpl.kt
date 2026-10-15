package io.tolgee.ee.service.aiReview

import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.component.CurrentDateProvider
import io.tolgee.dtos.aiReview.AiReviewRunResult
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.ee.service.prompt.DefaultPromptHelper
import io.tolgee.exceptions.AiReviewInvalidOutputException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.FailedDependencyException
import io.tolgee.exceptions.LlmRateLimitedException
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.aiReview.TranslationAiReview
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.PromptType
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.enums.aiReview.AiReviewOutcome
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.translation.Translation
import io.tolgee.repository.TranslationRepository
import io.tolgee.repository.aiReview.AiReviewRuleRepository
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.repository.qa.TranslationQaIssueRepository
import io.tolgee.service.PromptService
import io.tolgee.service.aiReview.AiReviewAvailability
import io.tolgee.service.aiReview.AiReviewBatchService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.aiReviewTextHash
import io.tolgee.util.executeInNewTransaction
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import java.util.Date

@Primary
@Service
class AiReviewBatchServiceImpl(
  private val projectService: ProjectService,
  private val aiReviewAvailability: AiReviewAvailability,
  private val translationRepository: TranslationRepository,
  private val translationAiReviewRepository: TranslationAiReviewRepository,
  private val aiReviewRuleRepository: AiReviewRuleRepository,
  private val translationQaIssueRepository: TranslationQaIssueRepository,
  private val triggerService: AiReviewTriggerService,
  private val promptService: PromptService,
  private val defaultPromptHelper: DefaultPromptHelper,
  private val translationService: TranslationService,
  private val languageService: LanguageService,
  private val entityManager: EntityManager,
  private val currentDateProvider: CurrentDateProvider,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val platformTransactionManager: PlatformTransactionManager,
) : AiReviewBatchService {
  private sealed interface RunOutcome {
    data class Scored(
      val result: AiReviewRunResult,
    ) : RunOutcome

    data class Failed(
      val errorCode: String,
      val errorDetail: String?,
    ) : RunOutcome
  }

  private data class Item(
    val project: ProjectDto,
    val translation: Translation,
    val row: TranslationAiReview,
  )

  override fun reviewItem(
    projectId: Long,
    item: BatchTranslationTargetItem,
  ) {
    val (project, translation, row) = findItem(projectId, item) ?: return
    review(project, translation, row)
  }

  override fun reevaluateItem(
    projectId: Long,
    item: BatchTranslationTargetItem,
  ) {
    val (project, translation, row) = findItem(projectId, item) ?: return
    reevaluate(project, translation, row)
  }

  private fun findItem(
    projectId: Long,
    item: BatchTranslationTargetItem,
  ): Item? {
    val project = projectService.getDto(projectId)
    if (!aiReviewAvailability.isAvailable(project.organizationOwnerId)) return null
    val translation =
      translationRepository
        .findAllWithKeyAndLanguageByKeyIdInAndLanguageIdIn(listOf(item.keyId), listOf(item.languageId))
        .firstOrNull() ?: return null
    val row = translationAiReviewRepository.findByTranslationId(translation.id) ?: return null
    return Item(project, translation, row)
  }

  private fun review(
    project: ProjectDto,
    translation: Translation,
    row: TranslationAiReview,
  ) {
    if (row.outcome != AiReviewOutcome.PENDING) return
    val hash = aiReviewTextHash(translation.text)
    if (row.textHash != hash || !isReviewable(project, translation, row)) {
      abandonPending(row)
      return
    }

    val languageId = translation.language.id
    val qaCovered = triggerService.isQaCovered(project, languageId)
    if (qaCovered && translation.qaChecksStale) return

    val reviewingMatch = matchReviewingRule(project.id, translation, row)
    if (reviewingMatch !is AiReviewRuleMatcher.Match.Review) {
      abandonPending(row)
      return
    }
    if (reviewingMatch.rule.skipWithOpenQaIssues && qaCovered && hasOpenQaIssues(translation)) {
      abandonPending(row)
      return
    }

    val claimedAt = claim(row, hash) ?: return
    try {
      val runOutcome = runReview(project.id, translation, reviewingMatch.rule)
      applyResult(project, translation, hash, reviewingMatch, runOutcome)
    } catch (e: Exception) {
      releaseClaim(row, hash, claimedAt)
      throw e
    }
  }

  private fun isReviewable(
    project: ProjectDto,
    translation: Translation,
    row: TranslationAiReview,
  ): Boolean {
    if (translation.language.id == languageService.getProjectBaseLanguage(project.id).id) return false
    if (translation.text.isNullOrBlank()) return false
    return translation.state == TranslationState.TRANSLATED || isRereviewOfApproved(translation, row)
  }

  private fun isRereviewOfApproved(
    translation: Translation,
    row: TranslationAiReview,
  ): Boolean = row.previousOutcome == AiReviewOutcome.AUTO_REVIEWED && translation.state == TranslationState.REVIEWED

  private fun abandonPending(row: TranslationAiReview) {
    triggerService.restoreOrDropPending(listOf(row.id))
  }

  private fun matchRules(
    projectId: Long,
    translation: Translation,
    activeOnly: Boolean = false,
  ): AiReviewRuleMatcher.Match {
    val rules =
      aiReviewRuleRepository
        .findAllEnabledByProjectId(projectId)
        .filter { !activeOnly || it.mode == AiReviewRuleMode.ACTIVE }
    val scope =
      triggerService.getKeyScopes(listOf(translation.key.id))[translation.key.id]
        ?: return AiReviewRuleMatcher.Match.NoRule
    return AiReviewRuleMatcher.match(rules, scope, translation.language.id)
  }

  private fun matchReviewingRule(
    projectId: Long,
    translation: Translation,
    row: TranslationAiReview,
  ): AiReviewRuleMatcher.Match {
    val match = matchRules(projectId, translation)
    if (match !is AiReviewRuleMatcher.Match.Review || !isRereviewOfApproved(translation, row)) return match
    return matchRules(projectId, translation, activeOnly = true)
  }

  private fun hasOpenQaIssues(translation: Translation): Boolean =
    translationQaIssueRepository.countByTranslationIdAndState(translation.id, QaIssueState.OPEN) > 0

  private fun claim(
    row: TranslationAiReview,
    hash: String,
  ): Date? {
    val now = currentDateProvider.date
    val claimed =
      executeInNewTransaction(platformTransactionManager) {
        translationAiReviewRepository.claimPending(
          id = row.id,
          textHash = hash,
          now = now,
          claimExpiredBefore = Date(now.time - CLAIM_EXPIRATION_MS),
        )
      }
    return now.takeIf { claimed > 0 }
  }

  private fun releaseClaim(
    row: TranslationAiReview,
    hash: String,
    claimedAt: Date,
  ) {
    executeInNewTransaction(platformTransactionManager) {
      translationAiReviewRepository.releaseOwnClaim(row.id, hash, claimedAt)
    }
  }

  private fun runReview(
    projectId: Long,
    translation: Translation,
    rule: AiReviewRule,
  ): RunOutcome {
    try {
      val prompt =
        rule.prompt?.let { promptService.findPromptOrDefaultDto(projectId, it.id, PromptType.REVIEW) }
          ?: defaultPromptHelper.getDefaultReviewPrompt()
      val result =
        promptService.review(
          projectId,
          prompt.toReviewRunDto(translation.key.id, translation.language.id),
          LlmProviderPriority.LOW,
        )
      return RunOutcome.Scored(result)
    } catch (e: LlmRateLimitedException) {
      throw e
    } catch (e: AiReviewInvalidOutputException) {
      return RunOutcome.Failed(e.code, e.rawResponse)
    } catch (e: OutOfCreditsException) {
      return RunOutcome.Failed(e.reason.tolgeeMessage.code, e.message)
    } catch (e: FailedDependencyException) {
      return RunOutcome.Failed(e.code, e.cause?.message ?: e.params?.joinToString())
    } catch (e: BadRequestException) {
      return RunOutcome.Failed(e.code, e.params?.joinToString())
    }
  }

  private fun applyResult(
    project: ProjectDto,
    translation: Translation,
    hash: String,
    match: AiReviewRuleMatcher.Match.Review,
    runOutcome: RunOutcome,
  ) {
    val row = lockRowIfStillCurrent(translation, hash, listOf(AiReviewOutcome.PENDING)) ?: return
    val rereview = isRereviewOfApproved(translation, row)
    if (translation.state != TranslationState.TRANSLATED && !rereview) {
      translationAiReviewRepository.delete(row)
      return
    }

    if (rereview && runOutcome is RunOutcome.Failed) {
      abandonPending(row)
      return
    }

    val rule = match.rule
    row.rule = rule
    row.prompt = rule.prompt
    row.threshold = match.threshold
    row.dryRun = rule.mode == AiReviewRuleMode.DRY_RUN
    row.reviewedAt = currentDateProvider.date
    row.previousOutcome = null
    row.runBatchJobId = null
    row.resolvedBy = null
    row.resolvedAt = null

    when (runOutcome) {
      is RunOutcome.Failed -> {
        row.outcome = AiReviewOutcome.FAILED
        row.providerName = null
        row.score = null
        row.issues = null
        row.errorCode = runOutcome.errorCode
        row.errorDetail = runOutcome.errorDetail?.take(MAX_ERROR_DETAIL_LENGTH)
      }

      is RunOutcome.Scored -> {
        row.providerName = runOutcome.result.providerName
        row.score = runOutcome.result.score
        row.issues = runOutcome.result.issues.toMutableList()
        row.errorCode = null
        row.errorDetail = null
        row.outcome = decideScoredOutcome(row.dryRun, runOutcome.result.score, match.threshold)
      }
    }

    when {
      row.outcome == AiReviewOutcome.AUTO_REVIEWED -> {
        translationService.setStateBatch(translation, TranslationState.REVIEWED)
      }

      rereview -> {
        translationService.setStateBatch(translation, TranslationState.TRANSLATED)
      }
    }
    translationAiReviewRepository.save(row)
    publishUpdated(project.id, translation, row)
  }

  private fun decideScoredOutcome(
    dryRun: Boolean,
    score: Int,
    threshold: Int,
  ): AiReviewOutcome {
    if (score < threshold) return AiReviewOutcome.BELOW_THRESHOLD
    if (dryRun) return AiReviewOutcome.WOULD_AUTO_REVIEW
    return AiReviewOutcome.AUTO_REVIEWED
  }

  private fun reevaluate(
    project: ProjectDto,
    translation: Translation,
    row: TranslationAiReview,
  ) {
    if (row.outcome !in AiReviewOutcome.REEVALUATED) return
    val hash = aiReviewTextHash(translation.text)
    if (row.textHash != hash) return

    val match = matchRules(project.id, translation, activeOnly = row.outcome == AiReviewOutcome.AUTO_REVIEWED)
    val demotion = getDemotion(match, row.score) ?: return

    val lockedRow = lockRowIfStillCurrent(translation, hash, listOf(row.outcome)) ?: return
    if (lockedRow.outcome == AiReviewOutcome.AUTO_REVIEWED && translation.state == TranslationState.REVIEWED) {
      translationService.setStateBatch(translation, TranslationState.TRANSLATED)
    }
    lockedRow.outcome = AiReviewOutcome.BELOW_THRESHOLD
    lockedRow.rule = demotion.rule
    lockedRow.threshold = demotion.threshold
    translationAiReviewRepository.save(lockedRow)
    publishUpdated(project.id, translation, lockedRow)
  }

  private data class Demotion(
    val rule: AiReviewRule,
    val threshold: Int?,
  )

  private fun getDemotion(
    match: AiReviewRuleMatcher.Match,
    score: Int?,
  ): Demotion? =
    when (match) {
      is AiReviewRuleMatcher.Match.NoRule -> null
      is AiReviewRuleMatcher.Match.Never -> Demotion(match.rule, null)
      is AiReviewRuleMatcher.Match.Review -> getThresholdDemotion(match, score)
    }

  private fun getThresholdDemotion(
    match: AiReviewRuleMatcher.Match.Review,
    score: Int?,
  ): Demotion? {
    if (match.threshold <= (score ?: 0)) return null
    return Demotion(match.rule, match.threshold)
  }

  private fun lockRowIfStillCurrent(
    translation: Translation,
    hash: String,
    expectedOutcomes: Collection<AiReviewOutcome>,
  ): TranslationAiReview? {
    entityManager.refresh(translation, LockModeType.PESSIMISTIC_WRITE)
    if (aiReviewTextHash(translation.text) != hash) return null
    val row = findReviewRowBypassingPersistenceContext(translation.id) ?: return null
    if (row.outcome !in expectedOutcomes || row.textHash != hash) return null
    return row
  }

  private fun findReviewRowBypassingPersistenceContext(translationId: Long): TranslationAiReview? {
    entityManager.flush()
    translationAiReviewRepository.findByTranslationId(translationId)?.let { entityManager.detach(it) }
    return translationAiReviewRepository.findByTranslationId(translationId)
  }

  private fun publishUpdated(
    projectId: Long,
    translation: Translation,
    row: TranslationAiReview,
  ) {
    applicationEventPublisher.publishEvent(AiReviewUpdatedEvent.of(projectId, translation, row))
  }

  companion object {
    private const val MAX_ERROR_DETAIL_LENGTH = 2000
    private const val CLAIM_EXPIRATION_MS = 15 * 60 * 1000L
  }
}
