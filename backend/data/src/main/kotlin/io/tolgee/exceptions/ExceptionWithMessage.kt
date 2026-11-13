package io.tolgee.exceptions

import io.tolgee.constants.Message
import io.tolgee.constants.MessageInterface
import java.io.Serializable

abstract class ExceptionWithMessage(
  private val _code: String? = null,
  val params: List<Serializable?>? = null,
  cause: Throwable? = null,
) : RuntimeException("$_code $params", cause),
  ExceptionWithCode {
  var tolgeeMessage: Message? = null

  override val code: String
    get() = _code ?: tolgeeMessage?.code ?: throw IllegalStateException("Exception code or message not set")

  constructor(message: MessageInterface, params: List<Serializable?>? = null, cause: Throwable? = null) : this(
    message.code,
    params,
    cause,
  ) {
    this.tolgeeMessage = message as? Message
  }

  constructor(message: MessageInterface) : this(message.code, null) {
    this.tolgeeMessage = message as? Message
  }
}
