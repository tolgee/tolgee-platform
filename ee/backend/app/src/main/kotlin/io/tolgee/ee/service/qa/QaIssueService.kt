package io.tolgee.ee.service.qa

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.reporting.BusinessEventPublisher
import io.tolgee.component.reporting.OnBusinessEventToCaptureEvent
import io.tolgee.ee.data.qa.QaCheckIssueIgnoreRequest
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.qa.QaIssueModelAssembler
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.qa.TranslationQaIssue
import io.tolgee.model.translation.Translation
import io.tolgee.repository.qa.TranslationQaIssueRepository
import io.tolgee.service.translation.TranslationService
import io.tolgee.websocket.WebsocketEvent
import io.tolgee.websocket.WebsocketEventPublisher
import io.tolgee.websocket.WebsocketEventType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QaIssueService(
  private val qaIssueRepository: TranslationQaIssueRepository,
  private val translationService: TranslationService,
  private val websocketEventPublisher: WebsocketEventPublisher,
  private val currentDateProvider: CurrentDateProvider,
  private val qaIssueModelAssembler: QaIssueModelAssembler,
  private val businessEventPublisher: BusinessEventPublisher,
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
      toDelete.forEach { it.disableActivityLogging = true }
      if (toDelete.isNotEmpty()) qaIssueRepository.deleteAll(toDelete)
    }
    qaIssueRepository.flush()

    val entities =
      results.map { result ->
        val matchingExisting = existingIssues.find { existing -> result.matches(existing) }
        TranslationQaIssue(
          type = result.type,
          message = result.message,
          replacement = result.replacement,
          positionStart = result.positionStart,
          positionEnd = result.positionEnd,
          params = result.params,
          state = matchingExisting?.state ?: QaIssueState.OPEN,
          pluralVariant = result.pluralVariant,
          translation = translation,
        ).also { it.disableActivityLogging = true }
      }
    qaIssueRepository.saveAll(entities)
    return entities
  }

  @Transactional(readOnly = true)
  fun getIssuesForTranslation(
    projectId: Long,
    translationId: Long,
  ): List<TranslationQaIssue> {
    return qaIssueRepository.findAllByProjectIdAndTranslationId(projectId, translationId)
  }

  @Transactional(readOnly = true)
  fun getIssuesForTranslation(translationId: Long): List<TranslationQaIssue> {
    return qaIssueRepository.findByTranslationIds(listOf(translationId))
  }

  @Transactional
  fun ignoreIssue(
    projectId: Long,
    translationId: Long,
    issueId: Long,
  ) {
    val issue = getIssueByProjectAndTranslationAndId(projectId, translationId, issueId)
    issue.state = QaIssueState.IGNORED
    qaIssueRepository.save(issue)
    publishQaIssuesUpdated(issue.translation)
    publishQaIssueStateChange("QA_ISSUE_IGNORED", projectId, issue.type, issue.virtual)
  }

  @Transactional
  fun unignoreIssue(
    projectId: Long,
    translationId: Long,
    issueId: Long,
  ) {
    val issue = getIssueByProjectAndTranslationAndId(projectId, translationId, issueId)
    reopenOrDeleteIssue(issue)
    publishQaIssuesUpdated(issue.translation)
    publishQaIssueStateChange("QA_ISSUE_UNIGNORED", projectId, issue.type, issue.virtual)
  }

  fun getIssueByProjectAndTranslationAndId(
    projectId: Long,
    translationId: Long,
    issueId: Long,
  ): TranslationQaIssue {
    return qaIssueRepository.findByProjectIdAndTranslationIdAndId(projectId, translationId, issueId)
      ?: throw NotFoundException()
  }

  @Transactional
  fun ignoreIssueByParams(
    projectId: Long,
    translationId: Long,
    request: QaCheckIssueIgnoreRequest,
  ) {
    val issue = findMatchingIssue(projectId, translationId, request)
    val translation: Translation
    if (issue != null) {
      issue.state = QaIssueState.IGNORED
      qaIssueRepository.save(issue)
      translation = issue.translation
    } else {
      translation = translationService.get(projectId, translationId)
      val newIssue =
        TranslationQaIssue(
          type = request.type,
          message = request.message,
          replacement = request.replacement,
          positionStart = request.positionStart,
          positionEnd = request.positionEnd,
          params = request.params,
          state = QaIssueState.IGNORED,
          virtual = true,
          pluralVariant = request.pluralVariant,
          translation = translation,
        )
      qaIssueRepository.save(newIssue)
    }
    publishQaIssuesUpdated(translation)
    val isVirtual = issue?.virtual ?: true
    publishQaIssueStateChange("QA_ISSUE_IGNORED", projectId, request.type, isVirtual)
  }

  @Transactional
  fun unignoreIssueByParams(
    projectId: Long,
    translationId: Long,
    request: QaCheckIssueIgnoreRequest,
  ): Boolean {
    val issue = findMatchingIssue(projectId, translationId, request) ?: return false
    reopenOrDeleteIssue(issue)
    publishQaIssuesUpdated(issue.translation)
    publishQaIssueStateChange("QA_ISSUE_UNIGNORED", projectId, request.type, issue.virtual)
    return true
  }

  private fun reopenOrDeleteIssue(issue: TranslationQaIssue) {
    if (issue.virtual) {
      qaIssueRepository.delete(issue)
    } else {
      issue.state = QaIssueState.OPEN
      qaIssueRepository.save(issue)
    }
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
      request.pluralVariant,
    )
  }

  fun publishQaIssuesUpdated(translation: Translation) {
    val projectId = translation.key.project.id
    qaIssueRepository.flush()
    val allIssues = qaIssueRepository.findAllByTranslationId(translation.id)
    websocketEventPublisher(
      "/projects/$projectId/${WebsocketEventType.QA_ISSUES_UPDATED.typeName}",
      WebsocketEvent(
        data =
          mapOf(
            "translationId" to translation.id,
            "keyId" to translation.key.id,
            "languageTag" to translation.language.tag,
            "qaIssueCount" to allIssues.count { it.state == QaIssueState.OPEN },
            "qaChecksStale" to translation.qaChecksStale,
            "qaIssues" to allIssues.map { qaIssueModelAssembler.toModel(it) },
          ),
        timestamp = currentDateProvider.date.time,
      ),
    )
  }

  private fun publishQaIssueStateChange(
    eventName: String,
    projectId: Long,
    checkType: QaCheckType,
    virtual: Boolean,
  ) {
    businessEventPublisher.publish(
      OnBusinessEventToCaptureEvent(
        eventName = eventName,
        projectId = projectId,
        data =
          mapOf(
            "checkType" to checkType.name,
            "virtual" to virtual,
          ),
      ),
    )
  }

}
