package io.tolgee.model.translationMemory

/**
 * Projection interface for the TM list endpoint that includes stats.
 * Used with a native query in [io.tolgee.repository.translationMemory.TranslationMemoryRepository].
 */
interface TranslationMemoryWithStats {
  val id: Long
  val name: String
  val sourceLanguageTag: String
  val type: String
  val entryCount: Long
  val assignedProjectsCount: Long

  /** Comma-separated names of the first 3 assigned projects. */
  val assignedProjectNames: String?

  val defaultPenalty: Int

  val writeOnlyReviewed: Boolean
}
