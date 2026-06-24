package io.tolgee.ee.api.v2.hateoas.model.translationMemory

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.hateoas.organization.SimpleOrganizationModel
import io.tolgee.model.translationMemory.TranslationMemoryType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "translationMemories", itemRelation = "translationMemory")
class TranslationMemoryModel(
  val id: Long,
  val name: String,
  @Schema(
    description = "Source language tag of the translation memory",
    example = "en",
  )
  val sourceLanguageTag: String,
  val type: TranslationMemoryType,
  val organizationOwner: SimpleOrganizationModel,
  @Schema(
    description =
      "Default penalty (0–100) subtracted from match scores for every assignment " +
        "that does not define its own override.",
  )
  val defaultPenalty: Int = 0,
  @Schema(
    description =
      "When true, only translations in REVIEWED state contribute to this TM.",
  )
  val writeOnlyReviewed: Boolean = false,
) : RepresentationModel<TranslationMemoryModel>()
