package io.tolgee.hateoas.exportInfo

import io.tolgee.formats.ExportFormat
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "exportFormats", itemRelation = "exportFormat")
open class ExportFormatModel(
  val format: ExportFormat,
  val extension: String,
  val mediaType: String,
  val defaultFileStructureTemplate: String,
) : RepresentationModel<ExportFormatModel>()
