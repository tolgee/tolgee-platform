package io.tolgee.ee.service.qa

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.ee.data.qa.QaCheckIssueIgnoreRequest
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.qa.TranslationQaIssue
import io.tolgee.model.translation.Translation
import io.tolgee.repository.qa.TranslationQaIssueRepository
import io.tolgee.service.translation.TranslationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QaIssueService(
  private val qaIssueRepository: TranslationQaIssueRepository,
  private val objectMapper: ObjectMapper,
  private val translationService: TranslationService,
) {
  @Transactional
  fun replaceIssuesForTranslation(
    translation: Translation,
    results: List<QaCheckResult>,
    checkTypes: List<QaCheckType>? = null,
  ): List<TranslationQaIssue> {
    val existingIssues = qaIssueRepository.findAllByTranslationId(translation.id)

    if (checkTypes == null) {
      qaIssueRepository.deleteAllByTranslationId(translation.id)
    } else {
      val toDelete = existingIssues.filter { it.type in checkTypes }
      if (toDelete.isNotEmpty()) qaIssueRepository.deleteAll(toDelete)
    }
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
    return entities
  }

  fun getIssuesForTranslation(
    projectId: Long,
    translationId: Long,
  ): List<TranslationQaIssue> {
    return qaIssueRepository.findAllByProjectIdAndTranslationId(projectId, translationId)
  }

  fun getOpenIssuesForTranslation(translationId: Long): List<TranslationQaIssue> {
    return qaIssueRepository.findOpenByTranslationIds(listOf(translationId))
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
    if (issue.virtual) {
      qaIssueRepository.delete(issue)
      return
    }
    issue.state = QaIssueState.OPEN
    qaIssueRepository.save(issue)
  }

  fun getIssueByProjectAndId(
    projectId: Long,
    issueId: Long,
  ): TranslationQaIssue {
    return qaIssueRepository.findByProjectIdAndId(projectId, issueId)
      ?: throw io.tolgee.exceptions.NotFoundException()
  }

  @Transactional
  fun ignoreIssueByParams(
    projectId: Long,
    translationId: Long,
    request: QaCheckIssueIgnoreRequest,
  ) {
    val issue = findMatchingIssue(projectId, translationId, request)
    if (issue != null) {
      issue.state = QaIssueState.IGNORED
      qaIssueRepository.save(issue)
      return
    }

    val translation = translationService.get(projectId, translationId)
    val newIssue =
      TranslationQaIssue(
        type = request.type,
        message = request.message,
        replacement = request.replacement,
        positionStart = request.positionStart,
        positionEnd = request.positionEnd,
        params = request.params?.let { objectMapper.writeValueAsString(it) },
        state = QaIssueState.IGNORED,
        virtual = true,
        translation = translation,
      )
    qaIssueRepository.save(newIssue)
  }

  @Transactional
  fun unignoreIssueByParams(
    projectId: Long,
    translationId: Long,
    request: QaCheckIssueIgnoreRequest,
  ): Boolean {
    val issue = findMatchingIssue(projectId, translationId, request) ?: return false
    if (issue.virtual) {
      qaIssueRepository.delete(issue)
      return true
    }
    issue.state = QaIssueState.OPEN
    qaIssueRepository.save(issue)
    return true
  }

  private fun findMatchingIssue(
    projectId: Long,
    translationId: Long,
    request: QaCheckIssueIgnoreRequest,
  ): TranslationQaIssue? {
    return qaIssueRepository.findByProjectIdAndTranslationIdAndIssueParams(
      projectId,
      translationId,
      request.type,
      request.message,
      request.replacement,
      request.positionStart,
      request.positionEnd,
    )
  }

  fun deserializeParams(paramsJson: String?): Map<String, String>? {
    return paramsJson?.let { objectMapper.readValue<Map<String, String>>(it) }
  }
}
