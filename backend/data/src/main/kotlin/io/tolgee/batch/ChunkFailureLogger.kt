package io.tolgee.batch

import io.sentry.Sentry
import io.tolgee.exceptions.ExceptionWithCode
import io.tolgee.exceptions.ExpectedUserError
import io.tolgee.exceptions.LlmRateLimitedException
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.apache.commons.lang3.exception.ExceptionUtils

object ChunkFailureLogger : Logging {
  private val knownCauses: List<Class<out RuntimeException>> =
    listOf(
      OutOfCreditsException::class.java,
      LlmRateLimitedException::class.java,
    )

  fun log(exception: Throwable) {
    if (exception is MultipleItemsFailedException) {
      exception.exceptions
        .distinctBy { failureReason(it) }
        .forEach { logSingle(it) }
      return
    }
    logSingle(exception)
  }

  fun isExpectedUserError(exception: Throwable): Boolean {
    if (knownCauses.any { ExceptionUtils.indexOfType(exception, it) > -1 }) return true
    if (exception is MultipleItemsFailedException) {
      return exception.exceptions.isNotEmpty() && exception.exceptions.all { isExpectedUserError(it) }
    }
    if (exception.findUserLimitMessage() != null) return true
    return ExceptionUtils.getThrowableList(exception).any { it is ExpectedUserError }
  }

  private fun logSingle(exception: Throwable) {
    if (isExpectedUserError(exception)) {
      logger.info("Skipping Sentry capture for expected user error: ${describeCauseChain(exception)}")
      return
    }
    Sentry.captureException(exception)
    logger.error(exception.message, exception)
  }

  private fun failureReason(exception: ChunkItemFailedException): List<Any?> {
    val rootCause = ExceptionUtils.getRootCause(exception)
    val rootCauseCode = (rootCause as? ExceptionWithCode)?.let { runCatching { it.code }.getOrNull() }
    return listOf(exception.code, rootCause?.javaClass, rootCauseCode)
  }

  private fun describeCauseChain(exception: Throwable): String =
    ExceptionUtils.getThrowableList(exception).joinToString(" <- ") { "${it.javaClass.simpleName}: ${it.message}" }
}
