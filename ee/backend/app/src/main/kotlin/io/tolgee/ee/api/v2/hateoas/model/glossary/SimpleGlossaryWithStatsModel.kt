package io.tolgee.ee.api.v2.hateoas.model.glossary

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "glossaries", itemRelation = "glossary")
class SimpleGlossaryWithStatsModel(
  val id: Long,
  val name: String,
  val baseLanguageTag: String?,
  val firstAssignedProjectName: String?,
  val assignedProjectsCount: Long,
) : RepresentationModel<SimpleGlossaryWithStatsModel>()
