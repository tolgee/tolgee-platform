package io.tolgee.service

import io.tolgee.dtos.request.prompt.PromptRunDto
import io.tolgee.model.Prompt
import org.springframework.stereotype.Service

@Service
class PromptServiceOssImpl : PromptService {
  override fun translateAndUpdateTranslation(
    projectId: Long,
    data: PromptRunDto,
  ) {
    throw UnsupportedOperationException("Not included in OSS")
  }

  override fun findPrompt(
    projectId: Long,
    promtId: Long,
  ): Prompt {
    throw UnsupportedOperationException("Not included in OSS")
  }
}
