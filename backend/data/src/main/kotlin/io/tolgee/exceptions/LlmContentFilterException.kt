package io.tolgee.exceptions

import io.tolgee.constants.Message

class LlmContentFilterException(serviceName: String) : BadRequestException(
  Message.LLM_CONTENT_FILTER,
  params = listOf(serviceName)
)
