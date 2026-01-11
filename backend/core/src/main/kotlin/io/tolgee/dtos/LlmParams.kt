package io.tolgee.dtos

import io.tolgee.model.enums.LlmProviderPriority

class LlmParams(
  val messages: List<LlmMessage>,
  val shouldOutputJson: Boolean,
  val priority: LlmProviderPriority,
) {
  companion object {
    enum class LlmMessageType {
      TEXT,
      IMAGE,
    }

    class LlmMessage(
      val type: LlmMessageType,
      var text: String? = null,
      val image: String? = null,
    )
  }
}
