package io.tolgee.hateoas.dataImport

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "translations", itemRelation = "translation")
open class ImportTranslationModel(
  val id: Long,
  val text: String?,
  val keyName: String,
  val keyId: Long,
  val importedKeyDescription: String?,
  val existingKeyDescription: String?,
  val conflictId: Long?,
  val conflictText: String?,
  val override: Boolean,
  val resolved: Boolean,
  val isPlural: Boolean,
  val willBeConvertedToPlural: Boolean,
) : RepresentationModel<ImportTranslationModel>()
