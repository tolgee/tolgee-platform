package io.tolgee.dtos

class LLMParams(
  val messages: List<LlmMessage>,
) {
  companion object {
    enum class LlmMessageType {
      TEXT,
      IMAGE,
    }

    class LlmMessage(
      val type: LlmMessageType,
      val text: String? = null,
      val image: ByteArray? = null,
    )
  }
}
