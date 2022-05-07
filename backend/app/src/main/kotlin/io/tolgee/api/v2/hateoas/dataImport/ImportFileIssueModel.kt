package io.tolgee.api.v2.hateoas.dataImport

import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "importFileIssues", itemRelation = "importFileIssue")
open class ImportFileIssueModel(
  val id: Long,
  val type: FileIssueType,
  val params: List<ImportFileIssueParamModel>
) : RepresentationModel<ImportFileIssueModel>()
