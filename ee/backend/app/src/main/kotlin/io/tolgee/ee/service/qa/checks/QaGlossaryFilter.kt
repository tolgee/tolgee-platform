package io.tolgee.ee.service.qa.checks

import io.tolgee.ee.service.qa.QaCheckResult
import io.tolgee.ee.service.qa.QaGlossaryTerm

/**
 * Filters out results that overlap with glossary terms.
 */
fun filterByGlossaryTerms(
  results: List<QaCheckResult>,
  glossaryTerms: List<QaGlossaryTerm>?,
): List<QaCheckResult> {
  if (glossaryTerms.isNullOrEmpty() || results.isEmpty()) return results
  return results.filter { result ->
    glossaryTerms.none { term -> result.overlaps(term.start, term.end) }
  }
}
