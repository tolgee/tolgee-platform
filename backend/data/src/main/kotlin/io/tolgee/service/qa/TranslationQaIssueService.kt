package io.tolgee.service.qa

import io.tolgee.repository.qa.TranslationQaIssueRepository
import org.springframework.stereotype.Service

@Service
class TranslationQaIssueService(
  private val translationQaIssueRepository: TranslationQaIssueRepository,
) {
  fun getOpenIssueCountsByLanguageId(projectId: Long): Map<Long, Long> {
    return translationQaIssueRepository
      .getOpenIssueCountsByLanguageId(projectId)
      .associate { it.languageId to it.count }
  }
}
