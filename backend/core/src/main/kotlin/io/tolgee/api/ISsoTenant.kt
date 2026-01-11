package io.tolgee.api

interface ISsoTenant {
  val clientId: String
  val clientSecret: String
  val authorizationUri: String
  val domain: String
  val tokenUri: String

  /**
   * When true, users with an email matching the organization's domain must sign in using SSO
   */
  val force: Boolean
  val global: Boolean
}
