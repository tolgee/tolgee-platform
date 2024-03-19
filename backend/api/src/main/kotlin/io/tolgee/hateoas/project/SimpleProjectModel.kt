package io.tolgee.hateoas.project

import io.tolgee.dtos.Avatar
import io.tolgee.hateoas.language.LanguageModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "projects", itemRelation = "project")
open class SimpleProjectModel(
  val id: Long,
  val name: String,
  val description: String?,
  val slug: String?,
  val avatar: Avatar?,
  val baseLanguage: LanguageModel?,
  var icuPlaceholders: Boolean,
) : RepresentationModel<SimpleProjectModel>()
