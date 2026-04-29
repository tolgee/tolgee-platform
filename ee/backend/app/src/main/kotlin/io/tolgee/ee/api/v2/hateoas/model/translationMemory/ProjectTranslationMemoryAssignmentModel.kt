package io.tolgee.ee.api.v2.hateoas.model.translationMemory

import io.tolgee.model.translationMemory.TranslationMemoryType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(
  collectionRelation = "translationMemoryAssignments",
  itemRelation = "translationMemoryAssignment",
)
class ProjectTranslationMemoryAssignmentModel(
  val translationMemoryId: Long,
  val translationMemoryName: String,
  val sourceLanguageTag: String,
  val type: TranslationMemoryType,
  val readAccess: Boolean,
  val writeAccess: Boolean,
  val priority: Int,
  val defaultPenalty: Int = 0,
  val penalty: Int? = null,
  val writeOnlyReviewed: Boolean = false,
) : RepresentationModel<ProjectTranslationMemoryAssignmentModel>()
