package io.tolgee.batch

import io.tolgee.constants.Message
import io.tolgee.exceptions.ExceptionWithMessage

open class ChunkFailedException(
  message: Message,
  val successfulTargets: List<Any>,
  override val cause: Throwable,
) : ExceptionWithMessage(message)

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

open class CannotFinalizeActivityException(
  cause: Throwable,
) : ExceptionWithMessage(Message.CANNOT_FINALIZE_ACTIVITY, cause = cause)
