package io.tolgee.dtos.misc

data class EmailTemplateModel(
  val body: String,
  val placeholders: List<EmailPlaceholderModel>,
)
