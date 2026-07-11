package io.tolgee.exceptions

import io.tolgee.constants.Message

class LlmEmptyResponseException :
  FailedDependencyException(
    Message.LLM_PROVIDER_EMPTY_RESPONSE,
  )
