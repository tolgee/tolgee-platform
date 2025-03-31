package io.tolgee.configuration.tolgee.machineTranslation

import io.tolgee.configuration.annotations.DocProperty
import io.tolgee.model.enums.LLMProviderType
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.llm")
class LLMProperties {
  var providers: MutableList<LLMProvider> = mutableListOf()

  open class LLMProvider(
    @DocProperty("User visible provider name")
    override var name: String = "default",
    @DocProperty("Provider type (OPENAI or OLLAMA)")
    override var type: LLMProviderType,
    override var priority: String?,
    override var apiKey: String?,
    override var apiUrl: String?,
    override var model: String?,
    override var deployment: String?,
    override var keepAlive: String?,
    override var format: String?,
  ) : LLMProviderInterface
}
