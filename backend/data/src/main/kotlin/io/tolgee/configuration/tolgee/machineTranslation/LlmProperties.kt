package io.tolgee.configuration.tolgee.machineTranslation

import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.dtos.LlmProviderDto
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.LlmProviderType
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.llm")
class LlmProperties {
  var enabled: Boolean = false
  var providers: MutableList<LlmProvider> = mutableListOf()

  open class LlmProvider(
    @DocProperty("User visible provider name")
    override var name: String = "default",
    @DocProperty("Provider type (OPENAI or OLLAMA)")
    override var type: LlmProviderType,
    override var priority: LlmProviderPriority? = null,
    override var apiKey: String? = null,
    override var apiUrl: String,
    override var model: String? = null,
    override var deployment: String? = null,
    override var keepAlive: String? = null,
    override var format: String? = null,
    override var pricePerMillionInput: Double? = null,
    override var pricePerMillionOutput: Double? = null,
    override var attempts: List<Int>? = null,
  ) : LlmProviderInterface {
    fun toDto(id: Long): LlmProviderDto {
      return LlmProviderDto(
        id = id,
        name = name,
        type = type,
        priority = priority,
        apiKey = apiKey,
        apiUrl = apiUrl,
        model = model,
        deployment = deployment,
        keepAlive = keepAlive,
        format = format,
        pricePerMillionInput = pricePerMillionInput,
        pricePerMillionOutput = pricePerMillionOutput,
        attempts = attempts,
      )
    }
  }
}
