package io.tolgee.ee.service.qa

data class LanguageToolResponse(
  val matches: List<LanguageToolMatch> = emptyList(),
)
