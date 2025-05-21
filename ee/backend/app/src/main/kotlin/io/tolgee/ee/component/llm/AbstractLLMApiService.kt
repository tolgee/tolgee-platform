package io.tolgee.ee.component.llm

import io.tolgee.configuration.tolgee.machineTranslation.LLMProviderInterface
import io.tolgee.dtos.LLMParams
import io.tolgee.service.PromptService
import org.springframework.web.client.RestTemplate

val DEFAULT_ATTEMPTS = listOf(30)

abstract class AbstractLLMApiService {
  abstract fun translate(
    params: LLMParams,
    config: LLMProviderInterface,
    restTemplate: RestTemplate,
  ): PromptService.Companion.PromptResult

  /**
   * specify how many times and with what timeouts service should be called
   */
  open fun defaultAttempts(): List<Int> = DEFAULT_ATTEMPTS
}
