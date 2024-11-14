package io.tolgee.ee.data

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.ISsoTenant
import jakarta.validation.constraints.NotNull
import org.springframework.validation.annotation.Validated

@Validated
data class CreateProviderRequest(
  val enabled: Boolean,
  @field:NotNull
  override val clientId: String,
  @field:NotNull
  override val clientSecret: String,
  @field:NotNull
  override val authorizationUri: String,
  @field:NotNull
  override val tokenUri: String,
  @field:NotNull
  override val jwkSetUri: String,
  @field:NotNull
  override val domain: String,
) : ISsoTenant {
  @get:Schema(hidden = true)
  override val global: Boolean
    get() = false
}
