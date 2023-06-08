package io.tolgee.hateoas.pat

interface IPatModel {
  val id: Long
  val description: String
  val expiresAt: Long?
  val createdAt: Long
  val updatedAt: Long
  val lastUsedAt: Long?
}
