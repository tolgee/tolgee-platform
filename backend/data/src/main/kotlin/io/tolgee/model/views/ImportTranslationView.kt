package io.tolgee.model.views

interface ImportTranslationView {
  val id: Long
  val text: String?
  val keyName: String
  val keyId: Long
  val keyDescription: String?

  // there is some kind of Kotlin / Spring Issue when naming params with is* prefix
  val plural: Boolean
  val existingKeyPlural: Boolean?
  val conflictId: Long?
  val conflictText: String?
  val override: Boolean
  val resolvedHash: String?
}
