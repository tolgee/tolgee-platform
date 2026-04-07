package io.tolgee.ee.data.qa

import com.fasterxml.jackson.annotation.JsonValue

enum class QaPreviewMessageType(
  @JsonValue val value: String,
) {
  DONE("done"),
  RESULT("result"),
  ERROR("error"),
}
