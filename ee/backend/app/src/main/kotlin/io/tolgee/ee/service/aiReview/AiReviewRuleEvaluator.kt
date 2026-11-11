package io.tolgee.ee.service.aiReview

import io.tolgee.constants.Feature
import io.tolgee.ee.service.aiReview.matching.KeyScope
import io.tolgee.ee.service.aiReview.matching.RuleDecision
import io.tolgee.ee.service.aiReview.matching.RuleSnapshot
import io.tolgee.ee.service.aiReview.matching.StrictestRuleSelector
import io.tolgee.model.branching.Branch
import io.tolgee.model.translation.Translation
import io.tolgee.service.branching.BranchService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.project.ProjectService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AiReviewRuleEvaluator(
  private val aiReviewRuleCache: AiReviewRuleCache,
  private val branchService: BranchService,
  private val projectService: ProjectService,
  private val projectFeatureGuard: ProjectFeatureGuard,
) {
  @Transactional(readOnly = true)
  fun evaluate(
    translation: Translation,
    activeOnly: Boolean = false,
  ): RuleDecision =
    forProject(
      translation.key.project.id,
    ).evaluate(translation, activeOnly)

  @Transactional(readOnly = true)
  fun forProject(projectId: Long): ProjectRules =
    ProjectRules(loadRules(projectId), isBranchingEnabled(projectId), defaultBranchName(projectId))

  fun decider(projectId: Long): (KeyScope, Long) -> RuleDecision {
    val rules = loadRules(projectId)
    val branchingEnabled = isBranchingEnabled(projectId)
    return { scope, languageId -> StrictestRuleSelector.decide(rules, scope, languageId, branchingEnabled) }
  }

  fun loadRules(projectId: Long): List<RuleSnapshot> = aiReviewRuleCache.get(projectId)

  fun defaultBranchName(projectId: Long): String =
    branchService.getDefaultBranch(projectId)?.name ?: Branch.DEFAULT_BRANCH_NAME

  private fun isBranchingEnabled(projectId: Long): Boolean =
    projectFeatureGuard.isFeatureEnabled(Feature.BRANCHING, projectService.getDto(projectId))

  class ProjectRules(
    val rules: List<RuleSnapshot>,
    private val branchingEnabled: Boolean,
    private val defaultBranchName: String,
  ) {
    fun evaluate(
      translation: Translation,
      activeOnly: Boolean = false,
    ): RuleDecision {
      val key = translation.key
      val scope =
        KeyScope(
          keyId = key.id,
          namespace = key.namespace?.name ?: "",
          branchName = key.branch?.name ?: defaultBranchName,
          tags =
            key.keyMeta
              ?.tags
              ?.map { it.name }
              ?.toSet() ?: emptySet(),
        )
      return StrictestRuleSelector.decide(
        rules = rules,
        key = scope,
        languageId = translation.language.id,
        branchingEnabled = branchingEnabled,
        activeOnly = activeOnly,
      )
    }
  }
}
