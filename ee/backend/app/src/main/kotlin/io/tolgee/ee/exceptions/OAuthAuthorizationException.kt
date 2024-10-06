package io.tolgee.ee.exceptions

import io.tolgee.constants.Message
import io.tolgee.exceptions.ExpectedException

data class OAuthAuthorizationException(
  val msg: Message,
  val details: String? = null,
) : RuntimeException("${msg.code}: $details"), ExpectedException
