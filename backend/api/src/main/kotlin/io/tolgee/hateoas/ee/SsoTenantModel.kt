package io.tolgee.hateoas.ee

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "ssoTenants", itemRelation = "ssoTenant")
class SsoTenantModel(
  val authorizationUri: String,
  val clientId: String,
  val clientSecret: String,
  val redirectUri: String,
  val tokenUri: String,
  val isEnabled: Boolean,
  val jwkSetUri: String,
) : RepresentationModel<SsoTenantModel>(),
  Serializable
