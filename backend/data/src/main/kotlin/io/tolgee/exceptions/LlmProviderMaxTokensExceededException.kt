package io.tolgee.exceptions

import io.tolgee.constants.Message

class LlmProviderMaxTokensExceededException :
  FailedDependencyException(
    Message.LLM_PROVIDER_MAX_TOKENS_EXCEEDED,
  )
