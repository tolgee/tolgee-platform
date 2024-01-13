package io.tolgee.service.key

interface KeyWithBaseTranslationView {
  val id: Long
  val name: String
  val namespace: String?
  val baseTranslation: String
}
