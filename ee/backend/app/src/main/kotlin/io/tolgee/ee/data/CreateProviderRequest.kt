package io.tolgee.ee.data

import jakarta.validation.constraints.NotNull
import org.springframework.validation.annotation.Validated

@Validated
data class CreateProviderRequest(
  val name: String?,
  @field:NotNull
  val clientId: String,
  @field:NotNull
  val clientSecret: String,
  @field:NotNull
  val authorizationUri: String,
  @field:NotNull
  val tokenUri: String,
  @field:NotNull
  val jwkSetUri: String,
  val isEnabled: Boolean,
  @field:NotNull
  val domainName: String,
)
