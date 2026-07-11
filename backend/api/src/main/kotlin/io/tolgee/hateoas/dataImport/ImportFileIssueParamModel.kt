package io.tolgee.hateoas.dataImport

import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import io.tolgee.model.views.ImportFileIssueParamView
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "fileIssueParams", itemRelation = "fileIssueParamModel")
open class ImportFileIssueParamModel(
  override val type: FileIssueParamType,
  override val value: String?,
) : RepresentationModel<ImportFileIssueParamModel>(),
  ImportFileIssueParamView
