package io.tolgee.ee.api.v2.hateoas.model

import io.tolgee.model.enums.UserSessionType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "sessions", itemRelation = "session")
open class UserSessionModel(
  val id: Long,
  val type: UserSessionType,
  val ip: String?,
  val userAgent: String?,
  val countryCode: String?,
  val country: String?,
  val city: String?,
  val createdAt: Long,
  val lastUsedAt: Long?,
  val expiresAt: Long,
  val isCurrent: Boolean,
) : RepresentationModel<UserSessionModel>()
