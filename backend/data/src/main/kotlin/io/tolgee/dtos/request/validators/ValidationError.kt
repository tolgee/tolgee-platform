package io.tolgee.dtos.request.validators

import io.tolgee.constants.MessageInterface

class ValidationError(
  type: ValidationErrorType,
  message: MessageInterface,
  vararg parameters: String,
) {
  val parameters: Array<out String> = parameters
  val type: ValidationErrorType = type
  val message: MessageInterface = message
}
