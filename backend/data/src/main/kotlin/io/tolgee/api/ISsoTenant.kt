package io.tolgee.api

interface ISsoTenant {
  val clientId: String
  val clientSecret: String
  val authorizationUri: String
  val domain: String

  // val jwkSetUri: String
  val tokenUri: String
  val global: Boolean
}
