package io.tolgee.api

import io.tolgee.dtos.Avatar

interface IProjectActivityAuthorModel {
  val id: Long

  @Deprecated("A user's username (their e-mail) is only disclosed on the project members list.")
  val username: String?
  var name: String?
  var avatar: Avatar?
  var deleted: Boolean
}
