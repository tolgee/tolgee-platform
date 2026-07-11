package io.tolgee.hateoas.pat

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "pats", itemRelation = "pat")
open class PatModel(
  override val id: Long,
  override val description: String,
  override val expiresAt: Long?,
  override val createdAt: Long,
  override val updatedAt: Long,
  override val lastUsedAt: Long?,
) : RepresentationModel<PatModel>(),
  IPatModel
