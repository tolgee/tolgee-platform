package io.tolgee.ee.component.llm
import io.tolgee.configuration.tolgee.machineTranslation.LlmProviderInterface
import io.tolgee.dtos.LlmParams
import io.tolgee.dtos.PromptResult
import io.tolgee.ee.service.eeSubscription.EeSubscriptionServiceImpl
import io.tolgee.util.Logging
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class TolgeeApiService(
  private val subscriptionService: EeSubscriptionServiceImpl,
) : AbstractLlmApiService(), Logging {
  override fun defaultAttempts(): List<Int> = listOf(30)

  override fun translate(
    params: LlmParams,
    config: LlmProviderInterface,
    restTemplate: RestTemplate,
  ): PromptResult {
    val licenseKey =
      subscriptionService.findSubscriptionDto()?.licenseKey ?: throw IllegalStateException("Not Subscribed")
    val headers = HttpHeaders()
    headers.set("content-type", "application/json")
    headers.set("License-Key", licenseKey)

    val request = HttpEntity(params, headers)

    val url = "${config.apiUrl}/v2/public/llm/prompt"

    val response =
      restTemplate.exchange<PromptResult>(
        url,
        HttpMethod.POST,
        request,
      )

    return response.body ?: throw IllegalStateException("Response body is null")
  }
}
