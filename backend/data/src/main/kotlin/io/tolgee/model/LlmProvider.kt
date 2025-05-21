package io.tolgee.model

import io.tolgee.dtos.LlmProviderDto
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.LlmProviderType
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
class LlmProvider(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long = 0L,
  @field:NotBlank
  var name: String = "",
  @field:Enumerated(EnumType.STRING)
  var type: LlmProviderType = LlmProviderType.OPENAI,
  @field:Enumerated(EnumType.STRING)
  var priority: LlmProviderPriority? = null,
  var apiKey: String? = null,
  var apiUrl: String = "",
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
  fun toDto(): LlmProviderDto {
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
      attempts = null,
    )
  }
}
