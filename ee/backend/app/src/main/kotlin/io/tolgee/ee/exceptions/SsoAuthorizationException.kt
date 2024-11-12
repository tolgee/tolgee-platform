package io.tolgee.ee.exceptions

import io.tolgee.constants.Message
import io.tolgee.exceptions.AuthenticationException

class SsoAuthorizationException(msg: Message) : AuthenticationException(msg)
