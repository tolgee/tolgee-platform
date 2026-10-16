package io.tolgee.service.aiReview

import io.tolgee.repository.aiReview.AiReviewRuleRepository
import org.springframework.stereotype.Component

@Component
class AiReviewRuleNamespaceRenamer(
  private val aiReviewRuleRepository: AiReviewRuleRepository,
) {
  fun renameNamespace(
    projectId: Long,
    oldName: String,
    newName: String,
  ) {
    if (oldName == newName) return
    aiReviewRuleRepository.findAllByProjectId(projectId).forEach { rule ->
      if (oldName !in rule.namespaceIn && oldName !in rule.namespaceNotIn) return@forEach
      rule.namespaceIn = rule.namespaceIn.renamed(oldName, newName)
      rule.namespaceNotIn = rule.namespaceNotIn.renamed(oldName, newName)
      aiReviewRuleRepository.save(rule)
    }
  }

  private fun List<String>.renamed(
    oldName: String,
    newName: String,
  ): MutableList<String> =
    map { name ->
      when (name) {
        oldName -> newName
        else -> name
      }
    }.distinct().toMutableList()
}
