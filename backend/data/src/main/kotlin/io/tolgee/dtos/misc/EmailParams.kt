package io.tolgee.dtos.misc

class EmailParams(
  var to: String,
  var text: String,
  var subject: String,
  var attachments: List<EmailAttachment> = listOf(),
)
