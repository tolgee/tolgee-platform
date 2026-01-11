package io.tolgee.model.views

interface SimpleUserAccountView {
  val id: Long
  val name: String?
  val username: String
  val avatarHash: String?
}
