package io.tolgee.component.email.customTemplate.placeholder

import io.tolgee.component.email.customTemplate.EmailTemplateVariables

data class EmailPlaceholderDefinition(
  val position: Int,
  val placeholder: String,
  val description: String,
  val exampleValue: String,
)

data class EmailPlaceholderEntry<T : EmailTemplateVariables>(
  val definition: EmailPlaceholderDefinition,
  val accessor: (T) -> String?,
)
