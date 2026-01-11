package io.tolgee.api

import io.tolgee.dtos.Avatar

interface IProjectActivityAuthorModel {
  val id: Long
  val username: String?
  var name: String?
  var avatar: Avatar?
  var deleted: Boolean
}
