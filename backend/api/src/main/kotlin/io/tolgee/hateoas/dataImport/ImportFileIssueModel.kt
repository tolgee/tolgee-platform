package io.tolgee.hateoas.dataImport

import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.model.views.ImportFileIssueView
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "importFileIssues", itemRelation = "importFileIssue")
open class ImportFileIssueModel(
  override val id: Long,
  override val type: FileIssueType,
  override val params: List<ImportFileIssueParamModel>,
) : RepresentationModel<ImportFileIssueModel>(),
  ImportFileIssueView
