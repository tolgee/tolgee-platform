package io.tolgee.hateoas.ee

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
  override val jwkSetUri: String,
  override val domain: String,
) : ISsoTenant,
  RepresentationModel<SsoTenantModel>(),
  Serializable {
  override val global: Boolean
    get() = false
}
