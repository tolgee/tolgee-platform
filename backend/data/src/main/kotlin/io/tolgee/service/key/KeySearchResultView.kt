package io.tolgee.service.key

interface KeySearchResultView {
  val id: Long
  val namespace: String?
  val name: String
  val baseTranslation: String?
  val translation: String?
  val description: String?
}
