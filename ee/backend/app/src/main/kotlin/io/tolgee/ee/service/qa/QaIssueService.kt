package io.tolgee.ee.service.qa

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.reporting.BusinessEventPublisher
import io.tolgee.component.reporting.OnBusinessEventToCaptureEvent
import io.tolgee.ee.data.qa.QaCheckIssueIgnoreRequest
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.qa.QaIssueModelAssembler
import io.tolgee.model.ALLOCATION_SIZE
import io.tolgee.model.SEQUENCE_NAME
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.model.qa.TranslationQaIssue
import io.tolgee.model.translation.Translation
import io.tolgee.repository.qa.TranslationQaIssueRepository
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.SequenceIdProvider
import io.tolgee.websocket.WebsocketEvent
import io.tolgee.websocket.WebsocketEventPublisher
import io.tolgee.websocket.WebsocketEventType
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.sql.Types

@Service
class QaIssueService(
  private val qaIssueRepository: TranslationQaIssueRepository,
  private val translationService: TranslationService,
  private val websocketEventPublisher: WebsocketEventPublisher,
  private val currentDateProvider: CurrentDateProvider,
  private val qaIssueModelAssembler: QaIssueModelAssembler,
  private val businessEventPublisher: BusinessEventPublisher,
  private val jdbcTemplate: JdbcTemplate,
  private val objectMapper: ObjectMapper,
) {
  @Transactional
  fun replaceIssuesForTranslation(
    translationId: Long,
    results: List<QaCheckResult>,
    checkTypes: List<QaCheckType>? = null,
  ) {
    replaceIssuesForTranslations(
      resultsByTranslationId = mapOf(translationId to results),
      checkTypes = checkTypes,
    )
  }

  @Transactional
  fun replaceIssuesForTranslations(
    resultsByTranslationId: Map<Long, List<QaCheckResult>>,
    checkTypes: List<QaCheckType>? = null,
  ) {
    if (resultsByTranslationId.isEmpty()) return
    val translationIds = resultsByTranslationId.keys
    val existingIssuesByTranslationId = findExistingIssuesByTranslationIdsAndCheckTypes(translationIds, checkTypes)

    deleteByTranslationIdInAndTypeIn(translationIds, checkTypes)
    qaIssueRepository.flush()

    val newEntities =
      resultsByTranslationId.flatMap { (tId, results) ->
        val existingIssues = existingIssuesByTranslationId[tId].orEmpty()
        results.map { result ->
          val matchingExisting = existingIssues.find { result.matches(it) }
          val issueState = matchingExisting?.state ?: QaIssueState.OPEN
          QaIssueBulkInsert(tId, result, issueState)
        }
      }

    insertIssues(newEntities)
  }

  @Transactional(readOnly = true)
  fun getIssuesForTranslation(
    projectId: Long,
    translationId: Long,
  ): List<TranslationQaIssue> {
    return qaIssueRepository.findAllByProjectIdAndTranslationId(projectId, translationId)
  }

  @Transactional(readOnly = true)
  fun findExistingIssuesByTranslationIdsAndCheckTypes(
    translationIds: Collection<Long>,
    checkTypes: List<QaCheckType>? = null,
  ): Map<Long, List<TranslationQaIssue>> {
    if (translationIds.isEmpty()) return emptyMap()
    if (checkTypes == null) {
      return qaIssueRepository.findByTranslationIds(translationIds).groupBy { it.translation.id }
    }
    return qaIssueRepository.findByTranslationIdsAndCheckTypes(translationIds, checkTypes).groupBy { it.translation.id }
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

  /**
   * types == null -> all check types
   */
  @Transactional
  fun deleteByTranslationIdInAndTypeIn(
    translationIds: Collection<Long>,
    types: Collection<QaCheckType>? = null,
  ) {
    if (types == null) {
      qaIssueRepository.deleteAllByTranslationIdIn(translationIds)
      return
    }

    if (types.isEmpty()) {
      return
    }

    qaIssueRepository.deleteAllByTranslationIdInAndTypeIn(translationIds, types)
  }

  @Transactional
  fun insertIssues(rows: List<QaIssueBulkInsert>) {
    if (rows.isEmpty()) return

    val sequenceIdProvider = SequenceIdProvider(SEQUENCE_NAME, ALLOCATION_SIZE)
    jdbcTemplate.batchUpdate(
      """
      INSERT INTO translation_qa_issue (
        id, created_at, updated_at,
        translation_id, type, message, replacement,
        position_start, position_end, state, params, virtual, plural_variant
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false, ?)
      """.trimIndent(),
      rows,
      1000,
    ) { ps, row ->
      val r = row.result
      val id = sequenceIdProvider.next(ps.connection)
      ps.setLong(1, id)
      ps.setTimestamp(2, Timestamp(currentDateProvider.date.time))
      ps.setTimestamp(3, Timestamp(currentDateProvider.date.time))
      ps.setLong(4, row.translationId)
      ps.setString(5, r.type.name)
      ps.setString(6, r.message.name)
      ps.setObject(7, r.replacement, Types.VARCHAR)
      ps.setObject(8, r.positionStart, Types.INTEGER)
      ps.setObject(9, r.positionEnd, Types.INTEGER)
      ps.setString(10, row.state.name)
      ps.setObject(
        11,
        PGobject().apply {
          type = "jsonb"
          value = r.params?.let { objectMapper.writeValueAsString(it) }
        },
      )
      ps.setObject(12, r.pluralVariant, Types.VARCHAR)
    }
  }

  fun publishQaIssuesUpdated(translation: Translation) {
    val projectId = translation.key.project.id
    publishQaIssuesUpdated(
      projectId,
      listOf(
        QaIssuesUpdatedEvent(
          translation.id,
          translation.key.id,
          translation.language.tag,
          translation.qaChecksStale,
        ),
      ),
    )
  }

  fun publishQaIssuesUpdated(
    projectId: Long,
    events: Collection<QaIssuesUpdatedEvent>,
  ) {
    qaIssueRepository.flush()
    val data =
      events.withIssues().map { event ->
        val issues = event.issues?.map { qaIssueModelAssembler.toModel(it) } ?: emptyList()
        val issueCount = issues.count { it.state == QaIssueState.OPEN }
        mapOf(
          "translationId" to event.translationId,
          "keyId" to event.keyId,
          "languageTag" to event.languageTag,
          "qaIssueCount" to issueCount,
          "qaChecksStale" to event.qaChecksStale,
          "qaIssues" to issues,
        )
      }
    websocketEventPublisher(
      "/projects/$projectId/${WebsocketEventType.QA_ISSUES_UPDATED.typeName}",
      WebsocketEvent(
        data = data,
        timestamp = currentDateProvider.date.time,
      ),
    )
  }

  private fun Collection<QaIssuesUpdatedEvent>.withIssues(): Collection<QaIssuesUpdatedEvent> {
    val missing = filter { it.issues == null }.map { it.translationId }
    if (missing.isEmpty()) return this
    val issues = qaIssueRepository.findAllByTranslationIdIn(missing)
    val issuesByTranslationId = issues.groupBy { it.translation.id }
    return map {
      it.takeIf { it.issues != null } ?: it.copy(issues = issuesByTranslationId[it.translationId] ?: emptyList())
    }
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
