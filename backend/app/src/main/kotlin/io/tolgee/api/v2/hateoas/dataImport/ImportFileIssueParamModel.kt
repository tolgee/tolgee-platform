package io.tolgee.api.v2.hateoas.dataImport

import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "fileIssueParams", itemRelation = "fileIssueParamModel")
open class ImportFileIssueParamModel(
  val type: FileIssueParamType,
  val value: String?
) : RepresentationModel<ImportFileIssueParamModel>()
