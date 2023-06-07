package io.tolgee.exceptions

import io.tolgee.constants.Message
import java.io.Serializable

abstract class ExceptionWithMessage : RuntimeException {
  val params: List<Serializable?>?
  val code: String
  constructor(message: Message, params: List<Serializable?>?) : super(message.code) {
    this.params = params
    this.code = message.code
  }

  constructor(message: Message) : this(message, null)

  constructor(code: String, params: List<Serializable?>? = null) : super(code + params.toString()) {
    this.code = code
    this.params = params
  }
}
