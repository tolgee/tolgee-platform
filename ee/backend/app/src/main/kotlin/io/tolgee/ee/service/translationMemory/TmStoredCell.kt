package io.tolgee.ee.service.translationMemory

/**
 * Service-layer view of a stored translation in a TM row. Carries exactly what the row
 * assembler needs — keeps JPA `TranslationMemoryEntry` instances from leaving the service
 * boundary inside `TmRow.entries`.
 */
data class TmStoredCell(
  val entryId: Long,
  val targetText: String,
  val targetLanguageTag: String,
)
