package io.tolgee.dtos.misc

import java.util.Locale

class EmailParams(
  var to: String,
  var from: String? = null,
  var bcc: Array<String>? = null,
  var text: String? = null,
  var header: String? = null,
  var subject: String,
  var locale: Locale = Locale.ENGLISH,
  var attachments: List<EmailAttachment> = listOf(),
  var replyTo: String? = null,
  var templateName: String? = null,
  var recipientName: String? = null,
)
