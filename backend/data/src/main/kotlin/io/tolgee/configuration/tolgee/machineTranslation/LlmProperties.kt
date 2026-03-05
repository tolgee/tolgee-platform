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
    List of LLM providers. When `provider-defaults` is also set, list entries are merged with the
    matching map entry (by name). List values override map defaults only when explicitly set
    (non-null for nullable fields, non-default for `type` / `maxTokens`).
    `enabled` is always taken from the list entry.

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

  @DocProperty(
    description = """
    Map of provider defaults keyed by provider name. Use this to separate non-secret configuration
    (model, prices, type) from secrets (API keys) in Kubernetes deployments.

    ``` yaml
    provider-defaults:
      gpt-5-mini:
        type: OPENAI
        model: gpt-5-mini
        token-price-in-credits-input: 2.0
        token-price-in-credits-output: 1.5
    ```

    or using environment variables (in a ConfigMap):

    ```
    TOLGEE_LLM_PROVIDER_DEFAULTS_GPT_5_MINI_TYPE=OPENAI
    TOLGEE_LLM_PROVIDER_DEFAULTS_GPT_5_MINI_MODEL=gpt-5-mini
    TOLGEE_LLM_PROVIDER_DEFAULTS_GPT_5_MINI_TOKEN_PRICE_IN_CREDITS_INPUT=2.0
    TOLGEE_LLM_PROVIDER_DEFAULTS_GPT_5_MINI_TOKEN_PRICE_IN_CREDITS_OUTPUT=1.5
    ```

    Then supply only the API key via the `providers` list (in a Secret):

    ```
    TOLGEE_LLM_PROVIDERS_0_NAME=gpt-5-mini
    TOLGEE_LLM_PROVIDERS_0_API_KEY=sk-proj-...
    ```
  """,
  )
  var providerDefaults: MutableMap<String, LlmProviderDefaults> = mutableMapOf()

  @DocProperty(
    description = """
    Named fallback mapping. When a provider is not found, Tolgee will try the fallback provider.

    ``` yaml
    fallbacks:
      openai: anthropic
      anthropic: google-ai
    ```

    or using environment variables:

    ```
    TOLGEE_LLM_FALLBACKS_OPENAI=anthropic
    TOLGEE_LLM_FALLBACKS_ANTHROPIC=google-ai
    ```
  """,
  )
  var fallbacks: MutableMap<String, String> = mutableMapOf()

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

  class LlmProviderDefaults(
    @DocProperty(description = "Enable/disable provider")
    var enabled: Boolean = true,
    @DocProperty(description = "Provider type, an API type")
    var type: LlmProviderType = LlmProviderType.OPENAI,
    @DocProperty(description = "Provider API Key (optional for some providers)")
    var apiKey: String? = null,
    @DocProperty(description = "Provider API Url")
    var apiUrl: String? = null,
    @DocProperty(description = "Provider model (optional for some providers)")
    var model: String? = null,
    @DocProperty(description = "Provider deployment (optional for some providers)")
    var deployment: String? = null,
    @DocProperty(
      description = """Maximum number of tokens to generate.
      `max_completion_tokens` option for OpenAI API.
      `max_tokens` for Anthropic API.""",
    )
    var maxTokens: Long? = null,
    @DocProperty(description = "ChatGPT reasoning effort")
    var reasoningEffort: String? = null,
    @DocProperty(description = "Set to `json_schema` if the API supports JSON Schema")
    var format: String? = null,
    @DocProperty(
      description = "Load-balancing instruction HIGH = used for suggestions, LOW = used for batch operations",
    )
    var priority: LlmProviderPriority? = null,
    @DocProperty(
      description =
        "Specify attempts timeout(s) (Example: [30, 30] - Tolgee will make two attempts, each with timeout of 30s)",
    )
    var attempts: List<Int>? = null,
    @DocProperty(hidden = true)
    var tokenPriceInCreditsInput: Double? = null,
    @DocProperty(hidden = true)
    var tokenPriceInCreditsOutput: Double? = null,
  ) {
    fun toLlmProvider(name: String): LlmProvider {
      return LlmProvider(
        enabled = enabled,
        name = name,
        type = type,
        apiKey = apiKey,
        apiUrl = apiUrl,
        model = model,
        deployment = deployment,
        maxTokens = maxTokens ?: LlmProvider.MAX_TOKENS_DEFAULT,
        reasoningEffort = reasoningEffort,
        format = format,
        priority = priority,
        attempts = attempts,
        tokenPriceInCreditsInput = tokenPriceInCreditsInput,
        tokenPriceInCreditsOutput = tokenPriceInCreditsOutput,
      )
    }
  }
}
