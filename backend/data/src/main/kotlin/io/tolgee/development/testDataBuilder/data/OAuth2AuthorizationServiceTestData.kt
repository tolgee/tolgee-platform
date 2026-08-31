package io.tolgee.development.testDataBuilder.data

/** Two users beside the default one, so a grant query can be shown to answer for its own user only. */
class OAuth2AuthorizationServiceTestData : BaseTestData() {
  val userA = root.addUserAccount { username = "oauth_query_user_a" }.self

  val userB = root.addUserAccount { username = "oauth_query_user_b" }.self
}
