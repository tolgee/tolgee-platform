package io.tolgee.model.views

import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType

interface ImportFileIssueView {
  val id: Long
  val type: FileIssueType
  val params: List<ImportFileIssueParamView>
}
