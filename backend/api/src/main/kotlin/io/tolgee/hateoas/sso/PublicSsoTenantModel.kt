package io.tolgee.hateoas.sso

import org.springframework.hateoas.RepresentationModel

data class PublicSsoTenantModel(
  val domain: String,
  val global: Boolean,
  /**
   * When true, users with an email matching the organization's domain must sign in using SSO
   */
  val force: Boolean,
) : RepresentationModel<PublicSsoTenantModel>()
