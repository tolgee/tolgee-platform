package io.tolgee.hateoas.dataImport

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "translations", itemRelation = "translation")
open class ImportTranslationModel(
  val id: Long,
  val text: String?,
  val keyName: String,
  val keyId: Long,
  val conflictId: Long?,
  val conflictText: String?,
  val override: Boolean,
  val resolved: Boolean,
) : RepresentationModel<ImportTranslationModel>()
