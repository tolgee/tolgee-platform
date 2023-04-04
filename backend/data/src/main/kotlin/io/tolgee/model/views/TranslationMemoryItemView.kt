package io.tolgee.model.views

interface TranslationMemoryItemView {
  val baseTranslationText: String
  val targetTranslationText: String
  val keyName: String
  val keyNamespace: String?
  val similarity: Float
  val keyId: Long
}
