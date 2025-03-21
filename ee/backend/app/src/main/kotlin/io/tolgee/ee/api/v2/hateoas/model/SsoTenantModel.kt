package io.tolgee.ee.api.v2.hateoas.model

import io.tolgee.api.ISsoTenant
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "ssoTenants", itemRelation = "ssoTenant")
class SsoTenantModel(
  val enabled: Boolean,
  override val authorizationUri: String,
  override val clientId: String,
  override val clientSecret: String,
  override val tokenUri: String,
  /**
   * When true, users with an email matching the organization's domain must sign in using SSO
   */
  override val force: Boolean,
  override val domain: String,
) : ISsoTenant,
  RepresentationModel<SsoTenantModel>(),
  Serializable {
  override val global: Boolean
    get() = false
}
