package io.tolgee.hateoas.apiKey

interface IApiKeyModel {
  val id: Long
  var username: String?
  var userFullName: String?
  var projectId: Long
  var projectName: String
  var scopes: Set<String>
  val description: String
  val expiresAt: Long?
  val lastUsedAt: Long?
}
