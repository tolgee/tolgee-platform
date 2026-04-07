package io.tolgee.service.qa

import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.repository.qa.TranslationQaIssueRepository
import org.springframework.stereotype.Service

@Service
class TranslationQaIssueService(
  private val translationQaIssueRepository: TranslationQaIssueRepository,
) {
  fun getOpenIssueCountsByLanguageId(
    projectId: Long,
    branchId: Long? = null,
  ): Map<Long, Long> {
    return translationQaIssueRepository
      .getOpenIssueCountsByLanguageId(projectId, branchId)
      .associate { it.languageId to it.count }
  }

  fun getStaleCountsByLanguageId(
    projectId: Long,
    branchId: Long? = null,
  ): Map<Long, Long> {
    return translationQaIssueRepository
      .getStaleCountsByLanguageId(projectId, branchId)
      .associate { it.languageId to it.count }
  }

  fun getOpenIssueCountsByCheckType(
    projectId: Long,
    languageId: Long,
    branchId: Long? = null,
  ): Map<QaCheckType, Long> {
    return translationQaIssueRepository
      .getOpenIssueCountsByCheckType(projectId, languageId, branchId)
      .associate { it.checkType to it.count }
  }

  fun deleteAllByProjectId(projectId: Long) {
    translationQaIssueRepository.deleteAllByProjectId(projectId)
  }

  fun deleteAllByTranslationIdIn(translationIds: Collection<Long>) {
    translationQaIssueRepository.deleteAllByTranslationIdIn(translationIds)
  }
}
