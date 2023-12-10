package io.tolgee.testing

import io.tolgee.testing.mocking.MockWrappedBean
import org.mockito.AdditionalAnswers
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.web.client.RestTemplate

class TestOverridesConfiguration {
  @MockWrappedBean
  @Bean
  fun restTemplateMock(real: RestTemplate): RestTemplate {
    return Mockito.mock(RestTemplate::class.java, AdditionalAnswers.delegatesTo<RestTemplate>(real))
  }

  @MockWrappedBean("webhookRestTemplate")
  @Bean
  fun webhookRestTemplateMock(
    real: RestTemplate
  ): RestTemplate {
    return Mockito.mock(RestTemplate::class.java, AdditionalAnswers.delegatesTo<RestTemplate>(real))
  }
}
