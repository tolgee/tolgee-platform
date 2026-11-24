package io.tolgee.api.mcp

import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator

fun Validator.checkValid(request: Any) {
  val violations = validate(request)
  if (violations.isEmpty()) return
  throw ConstraintViolationException(violations)
}
