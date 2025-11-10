package io.tolgee.exceptions

import io.tolgee.constants.Message
import java.io.Serializable

class LlmRateLimitedException(
  val retryAt: Long? = null,
  params: List<Serializable?>? = null,
  cause: Exception? = null,
) : FailedDependencyException(
    Message.LLM_RATE_LIMITED,
    params,
    cause,
  )
