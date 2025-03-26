package io.tolgee.model

import io.tolgee.configuration.tolgee.machineTranslation.LLMProviderInterface
import io.tolgee.model.enums.LLMProviderType
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank

@Entity
class LLMProvider(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long = 0L,
  @field:NotBlank
  override var name: String = "",
  @field:Enumerated(EnumType.STRING)
  override var type: LLMProviderType,
  override var priority: String?,
  override var apiKey: String?,
  override var apiUrl: String?,
  override var model: String?,
  override var deployment: String?,
  override var keepAlive: String?,
  override var format: String?,
  @ManyToOne
  @JoinColumn(name = "organization_id")
  var organization: Organization,
) : AuditModel(), LLMProviderInterface
