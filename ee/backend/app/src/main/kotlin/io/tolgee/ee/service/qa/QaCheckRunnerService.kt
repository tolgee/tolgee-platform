package io.tolgee.ee.service.qa

import io.tolgee.model.enums.qa.QaIssueMessage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class QaCheckRunnerService(
  private val checks: List<QaCheck>,
) {
  fun runChecks(params: QaCheckParams): List<QaCheckResult> {
    return checks.flatMap { check ->
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
