package io.tolgee.model.translationMemory

interface TranslationMemoryWithStats {
  val id: Long
  val name: String
  val sourceLanguageTag: String
  val type: String
  val assignedProjectNamesCsv: String?
  val defaultPenalty: Int
  val writeOnlyReviewed: Boolean
}
