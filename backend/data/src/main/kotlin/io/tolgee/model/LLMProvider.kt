package io.tolgee.model

import io.tolgee.dtos.LLMProviderDto
import io.tolgee.model.enums.LLMProviderPriority
import io.tolgee.model.enums.LLMProviderType
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
class LLMProvider(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long = 0L,
  @field:NotBlank
  var name: String = "",
  @field:Enumerated(EnumType.STRING)
  var type: LLMProviderType = LLMProviderType.OPENAI,
  @field:Enumerated(EnumType.STRING)
  var priority: LLMProviderPriority? = null,
  var apiKey: String? = null,
  var apiUrl: String? = null,
  var model: String? = null,
  var deployment: String? = null,
  var keepAlive: String? = null,
  var format: String? = null,
  var pricePerMillionInput: Double? = null,
  var pricePerMillionOutput: Double? = null,
  @ManyToOne
  @JoinColumn(name = "organization_id")
  var organization: Organization,
) : AuditModel() {
  fun toDto(): LLMProviderDto {
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
      attempts = null,
    )
  }
}
