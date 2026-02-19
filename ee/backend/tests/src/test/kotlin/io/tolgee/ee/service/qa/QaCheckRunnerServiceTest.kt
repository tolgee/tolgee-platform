package io.tolgee.ee.service.qa

import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.model.enums.qa.QaIssueMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class QaCheckRunnerServiceTest {
  private val params =
    QaCheckParams(
      baseText = null,
      text = "Hello",
      baseLanguageTag = null,
      languageTag = "en",
    )

  @Test
  fun `returns fallback issue when check throws exception`() {
    val failingCheck =
      object : QaCheck {
        override val type = QaCheckType.EMPTY_TRANSLATION

        override fun check(params: QaCheckParams): List<QaCheckResult> {
          throw RuntimeException("Intentional test failure")
        }
      }
    val service = QaCheckRunnerService(listOf(failingCheck))

    val results = service.runChecks(params)

    assertThat(results).hasSize(1)
    assertThat(results[0].type).isEqualTo(QaCheckType.EMPTY_TRANSLATION)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_CHECK_FAILED)
    assertThat(results[0].replacement).isNull()
    assertThat(results[0].positionStart).isEqualTo(0)
    assertThat(results[0].positionEnd).isEqualTo(0)
  }

  @Test
  fun `other checks still return results when one check fails`() {
    val successResult =
      QaCheckResult(
        type = QaCheckType.EMPTY_TRANSLATION,
        message = QaIssueMessage.QA_EMPTY_TRANSLATION,
        replacement = null,
        positionStart = 0,
        positionEnd = 0,
      )
    val successfulCheck =
      object : QaCheck {
        override val type = QaCheckType.EMPTY_TRANSLATION

        override fun check(params: QaCheckParams): List<QaCheckResult> = listOf(successResult)
      }
    val failingCheck =
      object : QaCheck {
        override val type = QaCheckType.SPACES_MISMATCH

        override fun check(params: QaCheckParams): List<QaCheckResult> {
          throw RuntimeException("Intentional test failure")
        }
      }
    val service = QaCheckRunnerService(listOf(successfulCheck, failingCheck))

    val results = service.runChecks(params)

    assertThat(results).hasSize(2)
    assertThat(results[0].message).isEqualTo(QaIssueMessage.QA_EMPTY_TRANSLATION)
    assertThat(results[1].type).isEqualTo(QaCheckType.SPACES_MISMATCH)
    assertThat(results[1].message).isEqualTo(QaIssueMessage.QA_CHECK_FAILED)
  }
}
