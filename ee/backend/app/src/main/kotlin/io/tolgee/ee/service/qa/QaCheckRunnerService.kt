package io.tolgee.ee.service.qa

import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class QaCheckRunnerService(
  private val checks: List<QaCheck>,
  private val projectQaConfigService: ProjectQaConfigService,
) {
  fun runChecks(
    projectId: Long,
    params: QaCheckParams,
    checkTypes: List<QaCheckType>? = null,
  ): List<QaCheckResult> {
    val enabledTypes = projectQaConfigService.getEnabledCheckTypes(projectId)
    val typesToRun = if (checkTypes != null) enabledTypes.intersect(checkTypes.toSet()) else enabledTypes
    return checks
      .filter { it.type in typesToRun }
      .flatMap { check ->
        try {
          check.check(params)
        } catch (e: Exception) {
          logger.error("QA check ${check.type} failed", e)
          listOf(
            QaCheckResult(
              type = check.type,
              message = QaIssueMessage.QA_CHECK_FAILED,
              replacement = null,
              positionStart = 0,
              positionEnd = 0,
            ),
          )
        }
      }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(QaCheckRunnerService::class.java)
  }
}
