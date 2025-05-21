package io.tolgee.dtos

class LlmParams(
  val messages: List<LlmMessage>,
  val shouldOutputJson: Boolean,
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
