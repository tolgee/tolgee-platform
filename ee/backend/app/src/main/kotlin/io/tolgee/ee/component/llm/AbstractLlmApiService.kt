package io.tolgee.ee.component.llm

import io.tolgee.configuration.tolgee.machineTranslation.LlmProviderInterface
import io.tolgee.dtos.LlmParams
import io.tolgee.dtos.PromptResult
import org.springframework.web.client.RestTemplate

val DEFAULT_ATTEMPTS = listOf(30)

abstract class AbstractLlmApiService {
  abstract fun translate(
    params: LlmParams,
    config: LlmProviderInterface,
    restTemplate: RestTemplate,
  ): PromptResult

  /**
   * specify how many times and with what timeouts service should be called
   */
  open fun defaultAttempts(): List<Int> = DEFAULT_ATTEMPTS
}
