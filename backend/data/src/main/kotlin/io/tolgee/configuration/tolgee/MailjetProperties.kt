package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty

@DocProperty(prefix = "tolgee.mailjet")
class MailjetProperties {
  var apiKey: String? = null
  var secretKey: String? = null
  var contactListId: Long? = null
}
