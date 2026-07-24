package io.tolgee.hateoas.apiKey

interface IApiKeyModel {
  val id: Long

  @Deprecated("A user's username (their e-mail) is only disclosed on the project members list.")
  var username: String?
  var userFullName: String?
  var projectId: Long
  var projectName: String
  var scopes: Set<String>
  val description: String
  val expiresAt: Long?
  val lastUsedAt: Long?
}
