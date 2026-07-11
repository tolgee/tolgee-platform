package io.tolgee.ee.service.qa

import io.tolgee.ee.service.glossary.GlossaryTermService

data class QaGlossaryTerm(
  val start: Int,
  val end: Int,
  val termText: String,
)

fun GlossaryTermService.findQaGlossaryTerms(
  organizationId: Long,
  projectId: Long,
  text: String,
  languageTag: String,
): List<QaGlossaryTerm>? =
  getHighlights(organizationId, projectId, text, languageTag)
    .map { QaGlossaryTerm(start = it.position.start, end = it.position.end, termText = it.value.text) }
    .takeIf { it.isNotEmpty() }
