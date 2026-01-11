package io.tolgee.dtos.misc

import org.springframework.core.io.InputStreamSource

class EmailAttachment(
  var name: String,
  var inputStreamSource: InputStreamSource,
)
