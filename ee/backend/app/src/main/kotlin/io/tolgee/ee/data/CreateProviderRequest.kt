package io.tolgee.ee.data

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.ISsoTenant
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.validation.annotation.Validated

@Validated
data class CreateProviderRequest(
  val enabled: Boolean,
  override val force: Boolean,
  @field:NotNull
  @field:Size(max = 255)
  override val clientId: String,
  @field:NotNull
  @field:Size(max = 255)
  override val clientSecret: String,
  @field:NotNull
  @field:Size(max = 255)
  override val authorizationUri: String,
  @field:NotNull
  @field:Size(max = 255)
  override val tokenUri: String,
  @field:NotNull
  @field:Size(max = 255)
  override val domain: String,
) : ISsoTenant {
  @get:Schema(hidden = true)
  override val global: Boolean
    get() = false
}
