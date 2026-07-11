package io.tolgee.component.machineTranslation.metadata

data class TranslationGlossaryItem(
  val source: String,
  val target: String? = null,
  val description: String? = null,
  val isNonTranslatable: Boolean? = null,
  val isCaseSensitive: Boolean? = null,
  val isAbbreviation: Boolean? = null,
  val isForbiddenTerm: Boolean? = null,
)
