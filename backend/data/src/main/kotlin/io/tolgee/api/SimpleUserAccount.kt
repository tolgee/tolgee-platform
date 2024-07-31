package io.tolgee.api

interface SimpleUserAccount {
  val id: Long
  val username: String
  val name: String
  val deleted: Boolean
  val avatarHash: String?
}
