package io.tolgee.exceptions

import io.tolgee.constants.Message

class LlmProviderNotReturnedJsonException(
  cause: Exception? = null,
) : FailedDependencyException(
    Message.LLM_PROVIDER_NOT_RETURNED_JSON,
    cause = cause,
  )
