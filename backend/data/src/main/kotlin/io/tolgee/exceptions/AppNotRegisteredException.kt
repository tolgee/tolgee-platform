package io.tolgee.exceptions

import io.tolgee.constants.Message
import org.springframework.http.HttpStatus

/**
 * The manifest describes an app nobody has registered on this server yet. Its own error code — not a
 * generic bad request — is what lets the client offer to register the app instead of just failing.
 */
class AppNotRegisteredException : ErrorException {
  constructor(appId: String) : super(Message.APP_NOT_REGISTERED, listOf(appId))

  override val httpStatus: HttpStatus
    get() = HttpStatus.NOT_FOUND
}
