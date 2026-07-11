package io.tolgee.exceptions

import io.tolgee.constants.Message

class LlmContentFilterException :
  FailedDependencyException(
    Message.LLM_CONTENT_FILTER,
  )
