package io.tolgee.configuration.tolgee.machineTranslation

import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.dtos.LlmProviderDto
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.LlmProviderType
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.llm")
class LlmProperties {
  @DocProperty("Enable/disable AI translator (default: enabled if there is at least one provider)")
  var enabled: Boolean? = null
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
        tokenPriceInCreditsInput = tokenPriceInCreditsInput,
        tokenPriceInCreditsOutput = tokenPriceInCreditsOutput,
        attempts = attempts,
      )
    }
  }
}
