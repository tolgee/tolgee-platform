package io.tolgee.batch

import io.tolgee.constants.Message
import io.tolgee.exceptions.ExceptionWithMessage

open class ChunkFailedException(
  message: Message,
  val successfulTargets: List<Long>,
  override val cause: Throwable
) :
  ExceptionWithMessage(message)

open class FailedDontRequeueException(
  message: Message,
  successfulTargets: List<Long>,
  cause: Throwable
) : ChunkFailedException(message, successfulTargets, cause)

open class RequeueWithTimeoutException(
  message: Message,
  successfulTargets: List<Long>,
  cause: Throwable,
  val timeoutInMs: Int = 10000,
  val increaseFactor: Int = 10,
  val maxRetries: Int = 10
) : ChunkFailedException(message, successfulTargets, cause)
