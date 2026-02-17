package io.tolgee.ee.service.qa

import org.springframework.stereotype.Service

@Service
class QaCheckRunnerService(
  private val checks: List<QaCheck>,
) {
  fun runChecks(params: QaCheckParams): List<QaCheckResult> {
    return checks.flatMap { it.check(params) }
  }
}
