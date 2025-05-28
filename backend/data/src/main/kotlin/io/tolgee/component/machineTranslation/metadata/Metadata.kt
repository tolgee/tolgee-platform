package io.tolgee.component.machineTranslation.metadata

data class Metadata(
  val examples: List<ExampleItem> = emptyList(),
  val closeItems: List<ExampleItem> = emptyList(),
  val glossaryTerms: List<TranslationGlossaryItem> = emptyList(),
  val keyDescription: String?,
  val projectDescription: String?,
  val languageDescription: String?,
)
