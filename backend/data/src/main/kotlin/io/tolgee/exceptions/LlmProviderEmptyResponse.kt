package io.tolgee.exceptions

import io.tolgee.constants.Message

class LlmProviderEmptyResponseException() : BadRequestException(
  Message.LLM_PROVIDER_EMPTY_RESPONSE,
)
