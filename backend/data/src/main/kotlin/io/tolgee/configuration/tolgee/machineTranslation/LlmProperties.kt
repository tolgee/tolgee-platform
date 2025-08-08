package io.tolgee.configuration.tolgee.machineTranslation

import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.dtos.LlmProviderDto
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.LlmProviderType
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.llm")
@DocProperty(
  displayName = "LLM Providers",
)
class LlmProperties : MachineTranslationServiceProperties {
  @DocProperty("Enable/disable AI translator (default: enabled if there is at least one provider)")
  var enabled: Boolean? = null

  @DocProperty(description = "Whether llm-powered machine translation is enabled.")
  override var defaultEnabled: Boolean = true

  @DocProperty(description = "Whether to use llm machine translation as a primary translation engine.")
  override var defaultPrimary: Boolean = true

  @DocProperty(
    description = """
    List of LLM providers. Example:
    
    ```
    providers:
      - name: openai-gpt-4o-mini
        type: OPENAI
        api-key: "<api key>"
        api-url: "https://api.openai.com"
        model: gpt-4o-mini
        format: "json_schema"
    ```
    
    Check [llm providers documentation](/platform/projects_and_organizations/llm-providers#self-hosted-server-configuration) for more information.
  """
  )
  var providers: MutableList<LlmProvider> = mutableListOf()

  open class LlmProvider(
    @DocProperty("Enable/disable provider")
    var enabled: Boolean = true,
    @DocProperty("User visible provider name")
    override var name: String = "default",
    @DocProperty("Provider type, an API type")
    override var type: LlmProviderType,
    @DocProperty("Provider API Key (optional for some providers)")
    override var apiKey: String? = null,
    @DocProperty("Provider API Url")
    override var apiUrl: String? = null,
    @DocProperty("Provider model (optional for some providers)")
    override var model: String? = null,
    @DocProperty("Provider deployment (optional for some providers)")
    override var deployment: String? = null,
    @DocProperty(description = "ChatGPT reasoning effort")
    override var reasoningEffort: String? = null,
    override var format: String? = null,
    @DocProperty("Load-balancing instruction HIGH = used for suggestions, LOW = used for batch operations")
    override var priority: LlmProviderPriority? = null,
    @DocProperty(
      "Specify attempts timeout(s) (Example: [30, 30] - Tolgee will make two attempts, each with timeout of 30s)"
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
      )
    }
  }
}
