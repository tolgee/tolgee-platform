package io.tolgee.model.views

interface ImportTranslationView {
  val id: Long
  val text: String?
  val keyName: String
  val keyId: Long
  val conflictId: Long?
  val conflictText: String?
  val override: Boolean
  val resolvedHash: String?
}
