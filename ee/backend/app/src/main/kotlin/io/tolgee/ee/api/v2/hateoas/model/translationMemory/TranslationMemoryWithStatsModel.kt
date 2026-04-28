package io.tolgee.ee.api.v2.hateoas.model.translationMemory

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(
  collectionRelation = "translationMemories",
  itemRelation = "translationMemory",
)
class TranslationMemoryWithStatsModel(
  val id: Long,
  val name: String,
  @Schema(description = "Source language tag", example = "en")
  val sourceLanguageTag: String,
  @Schema(description = "PROJECT or SHARED", example = "SHARED")
  val type: String,
  @Schema(description = "Total number of entries in this translation memory")
  val entryCount: Long,
  @Schema(description = "Total number of projects using this translation memory")
  val assignedProjectsCount: Long,
  @Schema(description = "Names of the first 3 assigned projects")
  val assignedProjectNames: List<String>,
  @Schema(
    description =
      "Default penalty (0–100) subtracted from match scores unless an assignment overrides it.",
  )
  val defaultPenalty: Int = 0,
  @Schema(
    description = "When true, only REVIEWED translations contribute to this TM.",
  )
  val writeOnlyReviewed: Boolean = false,
) : RepresentationModel<TranslationMemoryWithStatsModel>()
