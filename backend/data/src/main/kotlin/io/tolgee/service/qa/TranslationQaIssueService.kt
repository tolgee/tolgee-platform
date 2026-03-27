package io.tolgee.service.qa

import io.tolgee.model.enums.qa.QaCheckType
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

  fun getStaleCountsByLanguageId(projectId: Long): Map<Long, Long> {
    return translationQaIssueRepository
      .getStaleCountsByLanguageId(projectId)
      .associate { it.languageId to it.count }
  }

  fun getOpenIssueCountsByCheckType(
    projectId: Long,
    languageId: Long,
  ): Map<QaCheckType, Long> {
    return translationQaIssueRepository
      .getOpenIssueCountsByCheckType(projectId, languageId)
      .associate { it.checkType to it.count }
  }

  fun deleteAllByProjectId(projectId: Long) {
    translationQaIssueRepository.deleteAllByProjectId(projectId)
  }
}
