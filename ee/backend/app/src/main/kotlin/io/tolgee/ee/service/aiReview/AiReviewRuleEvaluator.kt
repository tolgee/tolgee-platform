package io.tolgee.ee.service.aiReview

import io.tolgee.constants.Feature
import io.tolgee.ee.service.aiReview.matching.KeyScope
import io.tolgee.ee.service.aiReview.matching.RuleDecision
import io.tolgee.ee.service.aiReview.matching.RuleSnapshot
import io.tolgee.ee.service.aiReview.matching.StrictestRuleSelector
import io.tolgee.model.branching.Branch
import io.tolgee.model.translation.Translation
import io.tolgee.service.branching.BranchService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.project.ProjectService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AiReviewRuleEvaluator(
  private val aiReviewRuleCache: AiReviewRuleCache,
  private val keyScopeQuery: AiReviewKeyScopeQuery,
  private val languageService: LanguageService,
  private val branchService: BranchService,
  private val projectService: ProjectService,
  private val projectFeatureGuard: ProjectFeatureGuard,
) {
  data class DecisionCount(
    val languageId: Long,
    val decision: RuleDecision,
    val keyCount: Long,
  )

  fun loadRules(projectId: Long): List<RuleSnapshot> = aiReviewRuleCache.get(projectId)

  @Transactional(readOnly = true)
  fun evaluate(
    translation: Translation,
    activeOnly: Boolean = false,
  ): RuleDecision {
    val key = translation.key
    val projectId = key.project.id
    val scope =
      KeyScope(
        keyId = key.id,
        namespace = key.namespace?.name ?: "",
        branchName = key.branch?.name ?: defaultBranchName(projectId),
        tags =
          key.keyMeta
            ?.tags
            ?.map { it.name }
            ?.toSet() ?: emptySet(),
      )
    return StrictestRuleSelector.decide(
      rules = loadRules(projectId),
      key = scope,
      languageId = translation.language.id,
      branchingEnabled = isBranchingEnabled(projectId),
      activeOnly = activeOnly,
    )
  }

  @Transactional(readOnly = true)
  fun forEachDecision(
    projectId: Long,
    keyIds: Collection<Long>?,
    languageIds: Collection<Long>?,
    activeOnly: Boolean = false,
    fn: (keyId: Long, languageId: Long, decision: RuleDecision) -> Unit,
  ) {
    val rules = loadRules(projectId)
    val targetLanguageIds = targetLanguageIds(projectId, languageIds)
    val branchingEnabled = isBranchingEnabled(projectId)
    keyScopeQuery.forEachKeyScope(projectId, keyIds, defaultBranchName(projectId)) { scope ->
      targetLanguageIds.forEach { languageId ->
        fn(
          scope.keyId,
          languageId,
          StrictestRuleSelector.decide(rules, scope, languageId, branchingEnabled, activeOnly),
        )
      }
    }
  }

  @Transactional(readOnly = true)
  fun countDecisions(
    projectId: Long,
    keyIds: Collection<Long>?,
    languageIds: Collection<Long>?,
  ): List<DecisionCount> {
    val rules = loadRules(projectId)
    val targetLanguageIds = targetLanguageIds(projectId, languageIds)
    val branchingEnabled = isBranchingEnabled(projectId)
    val counts = mutableMapOf<Pair<Long, RuleDecision>, Long>()
    keyScopeQuery.groupKeyScopes(projectId, keyIds, defaultBranchName(projectId)).forEach { group ->
      targetLanguageIds.forEach { languageId ->
        val decision = StrictestRuleSelector.decide(rules, group.scope, languageId, branchingEnabled)
        counts.merge(languageId to decision, group.keyCount, Long::plus)
      }
    }
    return counts.map { (languageAndDecision, count) ->
      DecisionCount(languageAndDecision.first, languageAndDecision.second, count)
    }
  }

  private fun targetLanguageIds(
    projectId: Long,
    languageIds: Collection<Long>?,
  ): List<Long> =
    languageService
      .getProjectLanguages(projectId)
      .filter { !it.base }
      .map { it.id }
      .filter { languageIds == null || it in languageIds }

  private fun defaultBranchName(projectId: Long): String =
    branchService.getDefaultBranch(projectId)?.name ?: Branch.DEFAULT_BRANCH_NAME

  private fun isBranchingEnabled(projectId: Long): Boolean =
    projectFeatureGuard.isFeatureEnabled(Feature.BRANCHING, projectService.getDto(projectId))
}
