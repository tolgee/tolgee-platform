package io.tolgee.hateoas.dataImport

import io.tolgee.model.views.ImportLanguageView
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "languages", itemRelation = "language")
open class ImportLanguageModel(
  override val id: Long,
  override val name: String,
  override val existingLanguageId: Long?,
  override val existingLanguageTag: String?,
  @Deprecated("Use existingLanguageTag")
  val existingLanguageAbbreviation: String?,
  override val existingLanguageName: String?,
  override val importFileName: String,
  override val importFileId: Long,
  override val importFileIssueCount: Int,
  override val namespace: String?,
  override val totalCount: Int,
  override val conflictCount: Int,
  override val resolvedCount: Int,
) : RepresentationModel<ImportLanguageModel>(),
  ImportLanguageView
