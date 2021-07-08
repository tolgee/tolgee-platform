package io.tolgee.model.views

import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType

interface ImportFileIssueParamView {
  val type: FileIssueParamType
  val value: String?
}
