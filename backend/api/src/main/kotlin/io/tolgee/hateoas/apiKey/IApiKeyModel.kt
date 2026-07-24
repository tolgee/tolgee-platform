package io.tolgee.hateoas.apiKey

import io.tolgee.api.USERNAME_FIELD_DEPRECATION

interface IApiKeyModel {
  val id: Long

  @Deprecated(USERNAME_FIELD_DEPRECATION)
  val username: String?
  var userFullName: String?
  var projectId: Long
  var projectName: String
  var scopes: Set<String>
  val description: String
  val expiresAt: Long?
  val lastUsedAt: Long?
}
