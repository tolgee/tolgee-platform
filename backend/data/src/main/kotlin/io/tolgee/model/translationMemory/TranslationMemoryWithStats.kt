package io.tolgee.model.translationMemory

/**
 * Projection interface for the TM list endpoint that includes stats. Used with a native query
 * in [io.tolgee.repository.translationMemory.TranslationMemoryRepository].
 *
 * Assigned project names ride a CSV column for compact wire transport; the API model splits it
 * into a list. The list size doubles as the assigned-project count — no separate count column.
 */
interface TranslationMemoryWithStats {
  val id: Long
  val name: String
  val sourceLanguageTag: String
  val type: String

  val assignedProjectNamesCsv: String?

  val defaultPenalty: Int

  val writeOnlyReviewed: Boolean
}
