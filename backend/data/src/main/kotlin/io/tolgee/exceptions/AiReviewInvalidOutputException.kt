package io.tolgee.exceptions

import io.tolgee.constants.Message
import io.tolgee.dtos.PromptResult

class AiReviewInvalidOutputException(
  val rawResponse: String,
  val prompt: String? = null,
  val providerName: String? = null,
  val price: Int = 0,
  val usage: PromptResult.Usage? = null,
) : FailedDependencyException(Message.AI_REVIEW_INVALID_OUTPUT)
