package io.tolgee.batch

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.tolgee.component.automations.processors.WebhookExecutionFailed
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.OutOfCreditsException
import io.tolgee.exceptions.limits.PlanLimitExceededWordsException
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.IOException

class ChunkFailureLoggerTest {
  private val logger = LoggerFactory.getLogger(ChunkFailureLogger::class.java) as Logger
  private val appender = ListAppender<ILoggingEvent>()

  @BeforeEach
  fun attachAppender() {
    appender.start()
    logger.addAppender(appender)
  }

  @AfterEach
  fun detachAppender() {
    logger.detachAppender(appender)
  }

  @Test
  fun `logs out of credits as one info line without stack trace`() {
    ChunkFailureLogger.log(
      FailedDontRequeueException(
        Message.OUT_OF_CREDITS,
        listOf(),
        OutOfCreditsException(OutOfCreditsException.Reason.OUT_OF_CREDITS),
      ),
    )

    val event = appender.list.single()
    event.level.assert.isEqualTo(Level.INFO)
    event.throwableProxy.assert.isNull()
    event.formattedMessage.assert.contains("out_of_credits")
  }

  @Test
  fun `logs plan limit wrapped as generic item failure as info without stack trace`() {
    ChunkFailureLogger.log(
      ChunkItemFailedException(Message.TRANSLATION_FAILED, cause = PlanLimitExceededWordsException(30741, 30000)),
    )

    val event = appender.list.single()
    event.level.assert.isEqualTo(Level.INFO)
    event.throwableProxy.assert.isNull()
    event.formattedMessage.assert.contains("PlanLimitExceededWordsException: plan_word_limit_exceeded [30741, 30000]")
  }

  @Test
  fun `keeps the underlying cause of an expected error in the log line`() {
    ChunkFailureLogger.log(
      ChunkItemFailedException(
        Message.UNEXPECTED_ERROR_WHILE_EXECUTING_WEBHOOK,
        cause = WebhookExecutionFailed(IOException("connection refused")),
      ),
    )

    appender.list
      .single()
      .formattedMessage.assert
      .contains("connection refused")
  }

  @Test
  fun `logs items failing for the same reason once`() {
    val items =
      (1..5).map { ChunkItemFailedException(Message.TRANSLATION_FAILED, cause = IllegalStateException("boom $it")) }

    ChunkFailureLogger.log(MultipleItemsFailedException(items, listOf()))

    val event = appender.list.single()
    event.level.assert.isEqualTo(Level.ERROR)
    event.throwableProxy.assert.isNotNull()
  }

  @Test
  fun `logs items failing for different reasons separately`() {
    val items =
      listOf(
        ChunkItemFailedException(Message.TRANSLATION_FAILED, cause = IllegalStateException("boom")),
        ChunkItemFailedException(Message.TRANSLATION_FAILED, cause = IllegalArgumentException("bang")),
      )

    ChunkFailureLogger.log(MultipleItemsFailedException(items, listOf()))

    appender.list.assert.hasSize(2)
  }

  @Test
  fun `logs items failing with different error codes of the same class separately`() {
    val items =
      listOf(
        ChunkItemFailedException(Message.TRANSLATION_FAILED, cause = BadRequestException(Message.KEY_EXISTS)),
        ChunkItemFailedException(Message.TRANSLATION_FAILED, cause = BadRequestException(Message.KEY_NOT_FOUND)),
      )

    ChunkFailureLogger.log(MultipleItemsFailedException(items, listOf()))

    appender.list.assert.hasSize(2)
  }
}
