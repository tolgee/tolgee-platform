package io.tolgee.exceptions

import io.tolgee.constants.Message
import java.io.Serializable

/**
 * Dummy class to type error messages in swagger
 */
class ErrorResponseTyped(
  var code: Message,
  var params: List<Serializable?>?,
)
