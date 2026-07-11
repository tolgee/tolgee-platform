package io.tolgee.ee.service.translationMemory

/**
 * Service-layer view of a virtual cell — computed from a project translation rather than from
 * a stored TM entry. Virtual cells share the same shape across the content browser and TMX
 * export, so the type lives alongside [TmRow] in the service package.
 */
data class VirtualEntry(
  val sourceText: String,
  val targetText: String,
  val targetLanguageTag: String,
  val projectId: Long,
  val projectName: String,
  val keyName: String,
)
