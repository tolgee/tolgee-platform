package io.tolgee.ee.api.v2.hateoas.model.glossary

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "glossaries", itemRelation = "glossary")
class SimpleGlossaryWithStatsModel(
  val id: Long,
  val name: String,
  @Schema(
    description = "The primary language code used for terms (e.g., 'en' for English)",
    example = "en",
  )
  val baseLanguageTag: String,
  @Schema(
    description = "The name of one project using this glossary (can be shown as a preview)",
    example = "My Project",
  )
  val firstAssignedProjectName: String?,
  @Schema(
    description = "Total number of projects currently using this glossary",
    example = "69",
  )
  val assignedProjectsCount: Long,
) : RepresentationModel<SimpleGlossaryWithStatsModel>()
