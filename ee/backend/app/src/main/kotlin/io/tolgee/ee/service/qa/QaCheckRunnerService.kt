package io.tolgee.ee.service.qa

import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class QaCheckRunnerService(
  private val checks: List<QaCheck>,
  private val projectQaConfigService: ProjectQaConfigService,
) {
  fun runEnabledChecks(
    projectId: Long,
    params: QaCheckParams,
    checkTypes: List<QaCheckType>? = null,
    languageId: Long? = null,
  ): List<QaCheckResult> {
    val enabledTypes =
      if (languageId != null) {
        projectQaConfigService.getEnabledCheckTypesForLanguage(projectId, languageId)
      } else {
        projectQaConfigService.getEnabledCheckTypesForProject(projectId)
      }
    val typesToRun = if (checkTypes != null) enabledTypes.intersect(checkTypes.toSet()) else enabledTypes
    return checks
      .filter { it.type in typesToRun }
      .flatMap { check -> runCheck(check, params) }
  }

  fun runCheck(
    type: QaCheckType,
    params: QaCheckParams,
  ): List<QaCheckResult> {
    val check = findCheck(type)
    return runCheck(check, params)
  }

  suspend fun runCheckWithDebounce(
    type: QaCheckType,
    params: QaCheckParams,
  ): List<QaCheckResult> {
    val check = findCheck(type)

    val debounce = check.debounceDuration
    if (debounce != null) {
      delay(debounce)
    }

    return runCheck(check, params)
  }

  private fun runCheck(
    check: QaCheck,
    params: QaCheckParams,
  ): List<QaCheckResult> {
    try {
      return check.check(params)
    } catch (e: Exception) {
      logger.error("QA check ${check.type} failed", e)
      return listOf(
        QaCheckResult(
          type = check.type,
          message = QaIssueMessage.QA_CHECK_FAILED,
          replacement = null,
          positionStart = null,
          positionEnd = null,
        ),
      )
    }
  }

  private fun findCheck(type: QaCheckType): QaCheck {
    return checks.find { it.type == type } ?: throw IllegalStateException("Check $type not found")
  }

  companion object {
    private val logger = LoggerFactory.getLogger(QaCheckRunnerService::class.java)
  }
}
