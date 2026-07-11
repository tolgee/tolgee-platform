package io.tolgee.hateoas.dataImport

import io.tolgee.exceptions.ErrorResponseBody
import org.springframework.hateoas.PagedModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "fileIssues", itemRelation = "fileIssue")
open class ImportAddFilesResultModel(
  val errors: List<ErrorResponseBody>,
  val warnings: List<ErrorResponseBody>,
  val result: PagedModel<ImportLanguageModel>?,
) : RepresentationModel<ImportAddFilesResultModel>()
