package io.tolgee.unit

import io.tolgee.batch.RequeueWithDelayException
import io.tolgee.component.automations.processors.WebhookExecutionFailed
import io.tolgee.constants.Message
import io.tolgee.exceptions.ExpectedUserError
import org.apache.commons.lang3.exception.ExceptionUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException

/**
 * Locks in the assumption that webhook failures carry an [ExpectedUserError] somewhere
 * in the cause chain — that is the structural contract `ChunkProcessingUtil` relies on
 * to skip Sentry capture for user-misconfigured webhooks.
 */
class ExpectedUserErrorWiringTest {
  @Test
  fun `WebhookExecutionFailed implements ExpectedUserError`() {
    val ex = WebhookExecutionFailed(IOException("connection refused"))
    assertThat(ex).isInstanceOf(ExpectedUserError::class.java)
  }

  @Test
  fun `RequeueWithDelayException wrapping WebhookExecutionFailed exposes the marker via cause chain`() {
    val webhookFailure = WebhookExecutionFailed(IOException("connection refused"))
    val wrapped =
      RequeueWithDelayException(
        message = Message.UNEXPECTED_ERROR_WHILE_EXECUTING_WEBHOOK,
        cause = webhookFailure,
        delayInMs = 5000,
      )

    val hasMarker = ExceptionUtils.getThrowableList(wrapped).any { it is ExpectedUserError }
    assertThat(hasMarker).isTrue()
  }
}
