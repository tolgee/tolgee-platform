package io.tolgee.configuration.tolgee.machineTranslation

import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.dtos.LLMProviderDto
import io.tolgee.model.enums.LLMProviderPriority
import io.tolgee.model.enums.LLMProviderType
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.llm")
class LLMProperties {
  var enabled: Boolean = false
  var providers: MutableList<LLMProvider> = mutableListOf()

  open class LLMProvider(
    @DocProperty("User visible provider name")
    override var name: String = "default",
    @DocProperty("Provider type (OPENAI or OLLAMA)")
    override var type: LLMProviderType,
    override var priority: LLMProviderPriority? = null,
    override var apiKey: String? = null,
    override var apiUrl: String? = null,
    override var model: String? = null,
    override var deployment: String? = null,
    override var keepAlive: String? = null,
    override var format: String? = null,
    override var pricePerMillionInput: Double? = null,
    override var pricePerMillionOutput: Double? = null,
    override var attempts: List<Int>? = null,
  ) : LLMProviderInterface {
    fun toDto(id: Long): LLMProviderDto {
      return LLMProviderDto(
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
