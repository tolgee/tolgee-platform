package io.tolgee.ee.service.qa

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.qa.TranslationQaIssue
import io.tolgee.model.translation.Translation
import io.tolgee.repository.qa.TranslationQaIssueRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QaIssueService(
  private val qaIssueRepository: TranslationQaIssueRepository,
  private val objectMapper: ObjectMapper,
) {
  @Transactional
  fun replaceIssuesForTranslation(
    translation: Translation,
    results: List<QaCheckResult>,
  ) {
    val existingIssues = qaIssueRepository.findAllByTranslationId(translation.id)
    qaIssueRepository.deleteAllByTranslationId(translation.id)
    qaIssueRepository.flush()
    val entities =
      results.map { result ->
        val matchingExisting =
          existingIssues.find { existing ->
            existing.type == result.type &&
              existing.message == result.message &&
              existing.positionStart == result.positionStart &&
              existing.positionEnd == result.positionEnd
          }
        TranslationQaIssue(
          type = result.type,
          message = result.message,
          replacement = result.replacement,
          positionStart = result.positionStart,
          positionEnd = result.positionEnd,
          params = result.params?.let { objectMapper.writeValueAsString(it) },
          state = matchingExisting?.state ?: QaIssueState.OPEN,
          translation = translation,
        )
      }
    qaIssueRepository.saveAll(entities)
  }

  fun getIssuesForTranslation(
    projectId: Long,
    translationId: Long,
  ): List<TranslationQaIssue> {
    return qaIssueRepository.findAllByProjectAndTranslation(projectId, translationId)
  }

  @Transactional
  fun ignoreIssue(
    projectId: Long,
    issueId: Long,
  ) {
    val issue = getIssueByProjectAndId(projectId, issueId)
    issue.state = QaIssueState.IGNORED
    qaIssueRepository.save(issue)
  }

  @Transactional
  fun unignoreIssue(
    projectId: Long,
    issueId: Long,
  ) {
    val issue = getIssueByProjectAndId(projectId, issueId)
    issue.state = QaIssueState.OPEN
    qaIssueRepository.save(issue)
  }

  fun getIssueByProjectAndId(
    projectId: Long,
    issueId: Long,
  ): TranslationQaIssue {
    return qaIssueRepository.findByProjectAndId(projectId, issueId)
      ?: throw io.tolgee.exceptions.NotFoundException()
  }

  fun deserializeParams(paramsJson: String?): Map<String, String>? {
    return paramsJson?.let { objectMapper.readValue<Map<String, String>>(it) }
  }
}
