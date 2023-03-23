package io.tolgee.component.machineTranslation.metadata

data class Metadata(
  val examples: List<ExampleItem> = emptyList(),
  val bigMetaItems: List<BigMetaItem> = emptyList()
)
