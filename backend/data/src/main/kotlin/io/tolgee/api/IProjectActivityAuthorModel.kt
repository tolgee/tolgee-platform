package io.tolgee.api

import io.tolgee.dtos.Avatar

interface IProjectActivityAuthorModel {
  val id: Long

  @Deprecated(USERNAME_FIELD_DEPRECATION)
  val username: String?
  var name: String?
  var avatar: Avatar?
  var deleted: Boolean
}
