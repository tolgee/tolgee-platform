package io.tolgee.development.testDataBuilder.data

/** A user whose grants are revoked alongside their other credentials. */
class UserAccountOAuth2RevocationTestData : BaseTestData() {
  val subject = root.addUserAccount { username = "oauth_revocation_user" }.self
}
