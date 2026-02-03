package io.tolgee.batch

import io.tolgee.constants.Message
import io.tolgee.exceptions.ExceptionWithMessage

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

open class RequeueWithDelayException(
  message: Message,
  successfulTargets: List<Any> = listOf(),
  cause: Throwable,
  val delayInMs: Int = 100,
  val increaseFactor: Int = 10,
  val maxRetries: Int = 3,
) : ChunkFailedException(message, successfulTargets, cause)

open class MultipleItemsFailedException(
  val exceptions: List<RequeueWithDelayException>,
  override val successfulTargets: List<Any>,
) : ExceptionWithMessage(Message.MULTIPLE_ITEMS_IN_CHUNK_FAILED),
  HasSuccessfulTargets

open class CannotFinalizeActivityException(
  cause: Throwable,
) : ExceptionWithMessage(Message.CANNOT_FINALIZE_ACTIVITY, cause = cause)
