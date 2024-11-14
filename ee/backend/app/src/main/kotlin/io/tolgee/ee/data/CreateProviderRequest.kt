package io.tolgee.ee.data

import io.tolgee.api.ISsoTenant
import io.tolgee.model.Organization
import jakarta.validation.constraints.NotNull
import org.springframework.validation.annotation.Validated

@Validated
data class CreateProviderRequest(
  override val name: String,
  @field:NotNull
  override val clientId: String,
  @field:NotNull
  override val clientSecret: String,
  @field:NotNull
  override val authorizationUri: String,
  @field:NotNull
  override val tokenUri: String,
  @field:NotNull
  val jwkSetUri: String,
  val isEnabled: Boolean,
  @field:NotNull
  val domainName: String,
) : ISsoTenant {
  override val domain: String
    get() = TODO("Not yet implemented")
  override val jwtSetUri: String
    get() = TODO("Not yet implemented")
  override val global: Boolean
    get() = TODO("Not yet implemented")
  override val organization: Organization?
    get() = TODO("Not yet implemented")
}
