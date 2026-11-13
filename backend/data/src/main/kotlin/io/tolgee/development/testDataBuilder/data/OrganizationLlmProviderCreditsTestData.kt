package io.tolgee.development.testDataBuilder.data

import io.tolgee.constants.MtServiceType
import io.tolgee.model.Language
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.model.key.Key

class OrganizationLlmProviderCreditsTestData : BaseTestData() {
  lateinit var czech: Language
  lateinit var key: Key

  init {
    userAccountBuilder.defaultOrganizationBuilder.build {
      addLlmProvider {
        name = ORGANIZATION_PROVIDER
      }
      addLlmProvider {
        name = TOLGEE_TYPE_ORGANIZATION_PROVIDER
        type = LlmProviderType.TOLGEE
      }
    }

    projectBuilder.build {
      czech = addCzech().self
      key = addKey("key").self
      addTranslation {
        key = this@OrganizationLlmProviderCreditsTestData.key
        language = englishLanguage
        text = "Hello"
      }
      val organizationProviderPrompt =
        addPrompt {
          name = "Organization provider prompt"
          providerName = ORGANIZATION_PROVIDER
        }
      addMtServiceConfig {
        targetLanguage = czech
        primaryService = MtServiceType.PROMPT
        enabledServices = mutableSetOf(MtServiceType.PROMPT, MtServiceType.GOOGLE)
        prompt = organizationProviderPrompt.self
      }
    }
  }

  companion object {
    const val ORGANIZATION_PROVIDER = "organization-provider"
    const val TOLGEE_TYPE_ORGANIZATION_PROVIDER = "tolgee-type-organization-provider"
  }
}
