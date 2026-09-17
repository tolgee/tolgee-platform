package io.tolgee.unit

import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties
import io.tolgee.development.testDataBuilder.data.LlmProviderSourceTestData
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

/**
 * `serverConfigured` is what lets an LLM endpoint's outbound call reach a private address, so it has to follow the
 * provider's source. An organization's row is user-supplied, and reading it as the operator's own configuration
 * would hand any member of any organization a request originating inside the network.
 */
class LlmProviderServerConfiguredTest {
  @Test
  fun `a provider declared in the server configuration is server-configured`() {
    LlmProperties
      .LlmProvider(name = "srv", apiUrl = "http://ollama:11434")
      .toDto(1)
      .serverConfigured.assert
      .isTrue()
  }

  @Test
  fun `a provider stored on an organization is not`() {
    LlmProviderSourceTestData()
      .organizationProvider
      .toDto()
      .serverConfigured.assert
      .isFalse()
  }
}
