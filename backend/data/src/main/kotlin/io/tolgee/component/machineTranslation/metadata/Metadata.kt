package io.tolgee.component.machineTranslation.metadata

data class Metadata(
  val examples: List<ExampleItem> = emptyList(),
  val closeItems: List<ExampleItem> = emptyList(),
  val keyName: String?,
  val keyNamespace: String?
)
