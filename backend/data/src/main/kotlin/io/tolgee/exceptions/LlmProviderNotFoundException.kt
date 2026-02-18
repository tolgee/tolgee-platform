package io.tolgee.exceptions

import io.tolgee.constants.Message

class LlmProviderNotFoundException(
  providerName: String,
) : BadRequestException(Message.LLM_PROVIDER_NOT_FOUND, listOf(providerName))
