package io.tolgee.dtos

class LLMParams(
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
      val image: ByteArray? = null,
    )
  }
}
