package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty

@DocProperty(prefix = "tolgee.slack")
class SlackProperties {
  var token: String? = null
  var signingSecret: String? = null
  var clientId: String? = null
  var clientSecret: String? = null
}
