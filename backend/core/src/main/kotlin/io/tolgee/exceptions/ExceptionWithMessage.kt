package io.tolgee.exceptions

import io.tolgee.constants.Message
import java.io.Serializable

abstract class ExceptionWithMessage(
  private val _code: String? = null,
  val params: List<Serializable?>? = null,
  cause: Throwable? = null,
) : RuntimeException("$_code $params", cause) {
  var tolgeeMessage: Message? = null

  val code: String
    get() = _code ?: tolgeeMessage?.code ?: throw IllegalStateException("Exception code or message not set")

  constructor(message: Message, params: List<Serializable?>? = null, cause: Throwable? = null) : this(
    message.code,
    params,
    cause,
  ) {
    this.tolgeeMessage = message
  }

  constructor(message: Message) : this(message.code, null) {
    this.tolgeeMessage = message
  }
}
