package io.tolgee.exceptions

import java.io.Serializable

class ErrorResponseBody(
  var code: String,
  var params: List<Serializable?>?,
)
