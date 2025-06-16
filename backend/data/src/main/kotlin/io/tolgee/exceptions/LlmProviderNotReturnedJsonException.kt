package io.tolgee.exceptions

import io.tolgee.constants.Message

class LlmProviderNotReturnedJsonException : FailedDependencyException(
  Message.LLM_PROVIDER_NOT_RETURNED_JSON
)
