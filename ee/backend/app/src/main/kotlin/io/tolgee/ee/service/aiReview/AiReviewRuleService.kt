package io.tolgee.ee.service.aiReview

import io.tolgee.constants.Message
import io.tolgee.ee.data.aiReview.AiReviewRuleOverrideRequest
import io.tolgee.ee.data.aiReview.AiReviewRuleRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.aiReview.AiReviewRule
import io.tolgee.model.aiReview.AiReviewRuleLanguageOverride
import io.tolgee.model.enums.PromptType
import io.tolgee.model.enums.aiReview.AiReviewRuleMode
import io.tolgee.model.enums.aiReview.AiReviewStatus
import io.tolgee.repository.PromptRepository
import io.tolgee.repository.aiReview.AiReviewRuleRepository
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.service.language.LanguageService
import io.tolgee.service.queryBuilders.translationViewBuilder.WildcardLikeUtil
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AiReviewRuleService(
  private val aiReviewRuleRepository: AiReviewRuleRepository,
  private val aiReviewRuleCache: AiReviewRuleCache,
  private val languageService: LanguageService,
  private val promptRepository: PromptRepository,
  private val entityManager: EntityManager,
  private val aiReviewQueueService: AiReviewQueueService,
  private val translationAiReviewRepository: TranslationAiReviewRepository,
) {
  @Transactional(readOnly = true)
  fun list(projectId: Long): List<AiReviewRule> = aiReviewRuleRepository.findAllWithScopeByProjectId(projectId)

  @Transactional(readOnly = true)
  fun get(
    projectId: Long,
    ruleId: Long,
  ): AiReviewRule =
    aiReviewRuleRepository.findWithScopeByProjectIdAndId(projectId, ruleId)
      ?: throw NotFoundException(Message.AI_REVIEW_RULE_NOT_FOUND)

  @Transactional
  fun create(
    projectId: Long,
    request: AiReviewRuleRequest,
  ): AiReviewRule {
    val rule = AiReviewRule(project = entityManager.getReference(Project::class.java, projectId))
    applyRequest(projectId, rule, request)
    saveAndEvict(projectId, rule, startDemotion = rule.enabled)
    return rule
  }

  @Transactional
  fun update(
    projectId: Long,
    ruleId: Long,
    request: AiReviewRuleRequest,
  ): AiReviewRule {
    val rule = get(projectId, ruleId)
    val before = rule.toSnapshot()
    applyRequest(projectId, rule, request)
    saveAndEvict(projectId, rule, startDemotion = rule.enabled && rule.toSnapshot() != before)
    return rule
  }

  @Transactional
  fun setEnabled(
    projectId: Long,
    ruleId: Long,
    enabled: Boolean,
  ): AiReviewRule {
    val rule = get(projectId, ruleId)
    val wasEnabled = rule.enabled
    rule.enabled = enabled
    saveAndEvict(projectId, rule, startDemotion = enabled && !wasEnabled)
    return rule
  }

  @Transactional
  fun delete(
    projectId: Long,
    ruleId: Long,
  ) {
    val rule = get(projectId, ruleId)
    translationAiReviewRepository.deleteByRuleAndStatusesUnlessAiManaged(ruleId, TRANSIENT_STATUSES)
    aiReviewRuleRepository.delete(rule)
    aiReviewRuleCache.evict(projectId)
  }

  @Transactional
  fun switchRulesUsingPromptToDryRun(
    projectId: Long,
    promptId: Long,
  ) {
    val rules = aiReviewRuleRepository.findAllByProjectIdAndPromptId(projectId, promptId)
    rules.forEach { it.mode = AiReviewRuleMode.DRY_RUN }
    saveAllAndEvict(projectId, rules)
  }

  @Transactional
  fun detachPromptAndSwitchToDryRun(
    projectId: Long,
    promptId: Long,
  ) {
    val rules = aiReviewRuleRepository.findAllByProjectIdAndPromptId(projectId, promptId)
    rules.forEach {
      it.prompt = null
      it.mode = AiReviewRuleMode.DRY_RUN
    }
    saveAllAndEvict(projectId, rules)
  }

  fun countActiveRulesUsingPrompt(
    projectId: Long,
    promptId: Long,
  ): Long = aiReviewRuleRepository.countByProjectIdAndPromptIdAndMode(projectId, promptId, AiReviewRuleMode.ACTIVE)

  @Transactional
  fun removeLanguage(
    projectId: Long,
    languageId: Long,
  ) {
    val rules = aiReviewRuleRepository.findAllWithScopeByProjectId(projectId)
    rules.forEach { rule ->
      val hadLanguages = rule.languages.isNotEmpty()
      rule.languages.removeIf { it.id == languageId }
      rule.overrides.removeIf { it.language.id == languageId }
      // An empty language list means "all languages", so a delete must never leave a scoped rule enabled
      if (hadLanguages && rule.languages.isEmpty()) rule.enabled = false
    }
    saveAllAndEvict(projectId, rules)
  }

  @Transactional
  fun renameNamespace(
    projectId: Long,
    oldName: String,
    newName: String,
  ) {
    val rules =
      aiReviewRuleRepository
        .findAllByProjectIdOrderById(projectId)
        .filter { oldName in it.namespaceIn || oldName in it.namespaceNotIn }
    rules.forEach {
      it.namespaceIn = it.namespaceIn.renamed(oldName, newName)
      it.namespaceNotIn = it.namespaceNotIn.renamed(oldName, newName)
    }
    saveAllAndEvict(projectId, rules)
  }

  private fun List<String>.renamed(
    oldName: String,
    newName: String,
  ): MutableList<String> = map { name -> newName.takeIf { name == oldName } ?: name }.distinct().toMutableList()

  private fun saveAndEvict(
    projectId: Long,
    rule: AiReviewRule,
    startDemotion: Boolean,
  ) {
    aiReviewRuleRepository.save(rule)
    aiReviewRuleCache.evict(projectId)
    if (startDemotion) aiReviewQueueService.startDemotionJobForApprovals(projectId)
  }

  private fun saveAllAndEvict(
    projectId: Long,
    rules: Collection<AiReviewRule>,
  ) {
    aiReviewRuleRepository.saveAll(rules)
    aiReviewRuleCache.evict(projectId)
  }

  private fun applyRequest(
    projectId: Long,
    rule: AiReviewRule,
    request: AiReviewRuleRequest,
  ) {
    WildcardLikeUtil.validatePatterns(request.branchIn)
    WildcardLikeUtil.validatePatterns(request.branchNotIn)
    val languages = languagesOfProject(projectId, request.languageIds + request.overrides.map { it.languageId })

    rule.name = request.name
    rule.enabled = request.enabled
    rule.tagIn = request.tagIn.toMutableList()
    rule.tagNotIn = request.tagNotIn.toMutableList()
    rule.namespaceIn = request.namespaceIn.toMutableList()
    rule.namespaceNotIn = request.namespaceNotIn.toMutableList()
    rule.languages = request.languageIds.map { languages.getValue(it) }.toMutableSet()
    rule.branchIn = request.branchIn.toMutableList()
    rule.branchNotIn = request.branchNotIn.toMutableList()
    rule.threshold = request.threshold
    rule.never = request.never
    rule.mode = request.mode
    rule.prompt = request.promptId?.let { reviewPrompt(projectId, it) }
    rule.skipOpenQaIssues = request.skipOpenQaIssues
    applyOverrides(rule, request.overrides, languages)
  }

  private fun applyOverrides(
    rule: AiReviewRule,
    requests: List<AiReviewRuleOverrideRequest>,
    languages: Map<Long, Language>,
  ) {
    if (requests.map { it.languageId }.toSet().size != requests.size) {
      throw BadRequestException(Message.AI_REVIEW_DUPLICATE_LANGUAGE_OVERRIDE)
    }
    val existing = rule.overrides.associateBy { it.language.id }
    rule.overrides.removeIf { override -> requests.none { it.languageId == override.language.id } }
    requests.forEach { request ->
      val override =
        existing[request.languageId]
          ?: AiReviewRuleLanguageOverride(rule = rule, language = languages.getValue(request.languageId))
            .also { rule.overrides.add(it) }
      override.threshold = request.threshold
      override.never = request.never
    }
  }

  private fun languagesOfProject(
    projectId: Long,
    languageIds: Collection<Long>,
  ): Map<Long, Language> {
    val projectLanguages = languageService.getProjectLanguages(projectId).associateBy { it.id }
    return languageIds.distinct().associateWith { languageId ->
      val language = projectLanguages[languageId] ?: throw BadRequestException(Message.LANGUAGE_NOT_FROM_PROJECT)
      if (language.base) throw BadRequestException(Message.AI_REVIEW_RULE_BASE_LANGUAGE)
      entityManager.getReference(Language::class.java, languageId)
    }
  }

  private fun reviewPrompt(
    projectId: Long,
    promptId: Long,
  ) = (promptRepository.findPrompt(projectId, promptId) ?: throw NotFoundException(Message.PROMPT_NOT_FOUND)).also {
    if (it.type != PromptType.REVIEW) throw BadRequestException(Message.AI_REVIEW_PROMPT_MUST_BE_REVIEW_TYPE)
  }

  companion object {
    private val TRANSIENT_STATUSES =
      setOf(
        AiReviewStatus.DRY_RUN_PASS,
        AiReviewStatus.DRY_RUN_FAIL,
        AiReviewStatus.FAILED,
        AiReviewStatus.SKIPPED_QA_ISSUES,
      )
  }
}
