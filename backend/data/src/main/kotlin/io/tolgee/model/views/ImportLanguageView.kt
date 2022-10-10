package io.tolgee.model.views

interface ImportLanguageView {
  val id: Long
  val name: String
  val existingLanguageId: Long?
  val existingLanguageTag: String?
  val existingLanguageName: String?
  val importFileName: String
  val importFileId: Long
  val importFileIssueCount: Int
  val namespace: String?
  val totalCount: Int
  val conflictCount: Int
  val resolvedCount: Int
}
