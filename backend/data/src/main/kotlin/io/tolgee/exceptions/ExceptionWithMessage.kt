package io.tolgee.exceptions

import io.tolgee.constants.Message
import java.io.Serializable

abstract class ExceptionWithMessage(
  private val _code: String? = null,
  val params: List<Serializable?>?
) : RuntimeException("$_code $params") {

  var tolgeeMessage: Message? = null

  val code: String
    get() = _code ?: tolgeeMessage?.code ?: throw IllegalStateException("Exception code or message not set")

  constructor(message: Message, params: List<Serializable?>?) : this(message.code, params) {
    this.tolgeeMessage = message
  }

  constructor(message: Message) : this(null, null) {
    this.tolgeeMessage = message
  }
}
