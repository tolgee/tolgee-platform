package io.tolgee.ee.data

import jakarta.validation.constraints.NotEmpty
import org.springframework.validation.annotation.Validated

// TODO: check how validation between backend and frontend is handled
@Validated
data class CreateProviderRequest(
  val name: String?,
  @field:NotEmpty
  val clientId: String,
  @field:NotEmpty
  val clientSecret: String,
  @field:NotEmpty
  val authorizationUri: String,
  @field:NotEmpty
  val tokenUri: String,
  @field:NotEmpty
  val jwkSetUri: String,
  val isEnabled: Boolean,
  @field:NotEmpty
  val domainName: String,
)
