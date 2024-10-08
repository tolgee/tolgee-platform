package io.tolgee.ee.exceptions

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException

data class OAuthAuthorizationException(
  val msg: Message,
  val details: String? = null,
) : BadRequestException("${msg.code}: $details")
