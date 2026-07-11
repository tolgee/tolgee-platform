package io.tolgee.ee.service.qa

data class LanguageToolMatch(
  val message: String = "",
  val offset: Int = 0,
  val length: Int = 0,
  val replacements: List<LanguageToolReplacement> = emptyList(),
  val rule: LanguageToolRule = LanguageToolRule(),
)
