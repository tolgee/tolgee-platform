package io.tolgee.configuration.annotations

annotation class AdditionalDocsProperties(
  val properties: Array<DocProperty>,
  val global: Boolean = false,
)
