package io.tolgee.dtos.misc

class EmailParams(
  var to: String,
  var bcc: Array<String>? = null,
  var text: String,
  var subject: String,
  var attachments: List<EmailAttachment> = listOf(),
  var replyTo: String? = null,
)
