package io.tolgee.configuration.tolgee.machineTranslation

import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.dtos.LlmProviderDto
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.LlmProviderType
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.llm")
@DocProperty(
  name = "llm",
  displayName = "LLM Providers",
)
class LlmProperties : MachineTranslationServiceProperties {
  @DocProperty(description = "Enable/disable AI translator (default: enabled if there is at least one provider)")
  var enabled: Boolean? = null

  @DocProperty(description = "Whether llm-powered machine translation is enabled.")
  override var defaultEnabled: Boolean = true

  @DocProperty(description = "Whether to use llm machine translation as a primary translation engine.")
  override var defaultPrimary: Boolean = true

  @DocProperty(
    description = """
    List of LLM providers. Example:
    
    ``` yaml
    providers:
      - name: openai-gpt-4o-mini
        type: OPENAI
        api-key: "myApiKey"
        api-url: "https://api.openai.com"
        model: gpt-4o-mini
        format: "json_schema"
    ```
    
    or using environment variables:
    
    ```
    TOLGEE_LLM_PROVIDERS_0_NAME=MySuperDuperAI
    TOLGEE_LLM_PROVIDERS_0_TYPE=OPENAI
    TOLGEE_LLM_PROVIDERS_0_API_KEY=myApiKey
    TOLGEE_LLM_PROVIDERS_0_API_URL=https://api.openai.com
    TOLGEE_LLM_PROVIDERS_0_MODEL=gpt-4o-mini
    TOLGEE_LLM_PROVIDERS_0_FORMAT=json_schema
    ```
    
    Check [llm providers documentation](/platform/projects_and_organizations/llm-providers#self-hosted-server-configuration) for more information.
  """,
  )
  var providers: MutableList<LlmProvider> = mutableListOf()

  class LlmProvider(
    @DocProperty(description = "Enable/disable provider")
    var enabled: Boolean = true,
    @DocProperty(description = "User visible provider name")
    override var name: String = "default",
    @DocProperty(description = "Provider type, an API type")
    override var type: LlmProviderType = LlmProviderType.OPENAI,
    @DocProperty(description = "Provider API Key (optional for some providers)")
    override var apiKey: String? = null,
    @DocProperty(description = "Provider API Url")
    override var apiUrl: String? = null,
    @DocProperty(description = "Provider model (optional for some providers)")
    override var model: String? = null,
    @DocProperty(description = "Provider deployment (optional for some providers)")
    override var deployment: String? = null,
    @DocProperty(
      description = """Maximum number of tokens to generate. 
      `max_completion_tokens` option for OpenAI API.
      `max_tokens` for Anthropic API.`""",
    )
    override var maxTokens: Long = MAX_TOKENS_DEFAULT,
    @DocProperty(description = "ChatGPT reasoning effort")
    override var reasoningEffort: String? = null,
    @DocProperty(description = "Set to `json_schema` if the API supports JSON Schema")
    override var format: String? = null,
    @DocProperty(
      description = "Load-balancing instruction HIGH = used for suggestions, LOW = used for batch operations",
    )
    override var priority: LlmProviderPriority? = null,
    @DocProperty(
      description =
        "Specify attempts timeout(s) (Example: [30, 30] - Tolgee will make two attempts, each with timeout of 30s)",
    )
    override var attempts: List<Int>? = null,
    @DocProperty(hidden = true)
    override var tokenPriceInCreditsInput: Double? = null,
    @DocProperty(hidden = true)
    override var tokenPriceInCreditsOutput: Double? = null,
  ) : LlmProviderInterface {
    fun toDto(id: Long): LlmProviderDto {
      return LlmProviderDto(
        id = id,
        name = name,
        type = type,
        priority = priority,
        apiKey = apiKey,
        rawApiUrl = apiUrl,
        model = model,
        deployment = deployment,
        format = format,
        reasoningEffort = reasoningEffort,
        tokenPriceInCreditsInput = tokenPriceInCreditsInput,
        tokenPriceInCreditsOutput = tokenPriceInCreditsOutput,
        attempts = attempts,
        maxTokens = maxTokens,
      )
    }

    companion object {
      const val MAX_TOKENS_DEFAULT: Long = 2000
    }
  }
}
