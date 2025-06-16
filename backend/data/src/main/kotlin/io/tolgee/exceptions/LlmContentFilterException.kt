package io.tolgee.exceptions

import io.tolgee.constants.Message

class LlmContentFilterException(serviceName: String) : FailedDependencyException(
  Message.LLM_PROVIDER_EMPTY_RESPONSE
)
