package io.tolgee.model

import io.tolgee.configuration.tolgee.machineTranslation.LlmProperties.LlmProvider.Companion.MAX_TOKENS_DEFAULT
import io.tolgee.dtos.LlmProviderDto
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.LlmProviderType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank

@Entity()
@Table(
  indexes = [
    Index(columnList = "organization_id"),
    Index(columnList = "name"),
  ],
)
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
  @Column(length = 511)
  var apiKey: String? = null,
  @Column(length = 2047)
  var apiUrl: String = "",
  var model: String? = null,
  var deployment: String? = null,
  var keepAlive: String? = null,
  var format: String? = null,
  var reasoningEffort: String? = null,
  @ManyToOne
  var organization: Organization,
) : AuditModel() {
  fun toDto(): LlmProviderDto {
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
      attempts = null,
      tokenPriceInCreditsInput = null,
      tokenPriceInCreditsOutput = null,
      maxTokens = MAX_TOKENS_DEFAULT,
    )
  }
}
