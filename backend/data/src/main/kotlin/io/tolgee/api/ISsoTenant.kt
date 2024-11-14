package io.tolgee.api

import io.tolgee.model.Organization

interface ISsoTenant {
  // TODO: Consider removing this property
  val name: String
  val clientId: String
  val clientSecret: String
  val authorizationUri: String
  val domain: String
  val jwtSetUri: String
  val tokenUri: String
  val global: Boolean
  val organization: Organization?
}
