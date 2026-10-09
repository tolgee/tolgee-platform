package io.tolgee.batch

import io.tolgee.constants.Message
import io.tolgee.exceptions.ExceptionWithMessage
import io.tolgee.exceptions.OutOfCreditsException
import org.apache.commons.lang3.exception.ExceptionUtils

interface HasSuccessfulTargets {
  val successfulTargets: List<Any>
}

open class ChunkFailedException(
  message: Message,
  override val successfulTargets: List<Any>,
  override val cause: Throwable,
) : ExceptionWithMessage(message),
  HasSuccessfulTargets

open class FailedDontRequeueException(
  message: Message,
  successfulTargets: List<Any>,
  cause: Throwable,
) : ChunkFailedException(message, successfulTargets, cause)

open class ChunkItemFailedException(
  message: Message,
  successfulTargets: List<Any> = listOf(),
  cause: Throwable,
  val delayInMs: Int = 100,
  val increaseFactor: Int = 10,
  val maxRetries: Int = 3,
) : ChunkFailedException(message, successfulTargets, cause)

open class MultipleItemsFailedException(
  val exceptions: List<ChunkItemFailedException>,
  override val successfulTargets: List<Any>,
) : ExceptionWithMessage(Message.MULTIPLE_ITEMS_IN_CHUNK_FAILED),
  HasSuccessfulTargets

open class CannotFinalizeActivityException(
  cause: Throwable,
) : ExceptionWithMessage(Message.CANNOT_FINALIZE_ACTIVITY, cause = cause)

private val USER_LIMIT_MESSAGES =
  setOf(
    Message.PLAN_TRANSLATION_LIMIT_EXCEEDED,
    Message.TRANSLATION_SPENDING_LIMIT_EXCEEDED,
    Message.PLAN_KEY_LIMIT_EXCEEDED,
    Message.KEYS_SPENDING_LIMIT_EXCEEDED,
    Message.PLAN_WORD_LIMIT_EXCEEDED,
    Message.WORDS_SPENDING_LIMIT_EXCEEDED,
  )

fun Throwable.findUserLimitMessage(): Message? =
  ExceptionUtils.getThrowableList(this).firstNotNullOfOrNull { throwable ->
    if (throwable is OutOfCreditsException) return@firstNotNullOfOrNull throwable.reason.tolgeeMessage
    val code = (throwable as? ExceptionWithMessage)?.let { runCatching { it.code }.getOrNull() }
    USER_LIMIT_MESSAGES.firstOrNull { it.code == code }
  }
