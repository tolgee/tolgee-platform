package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.LlmProvider

/** An organization's own LLM provider row: the counterpart of a provider declared in the server configuration. */
class LlmProviderSourceTestData : BaseTestData() {
  val organizationProvider: LlmProvider =
    userAccountBuilder.defaultOrganizationBuilder
      .addLlmProvider {
        name = "org-provider"
        apiUrl = "http://ollama:11434"
      }.self
}
