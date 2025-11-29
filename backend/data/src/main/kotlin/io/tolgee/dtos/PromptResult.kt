package io.tolgee.dtos

class PromptResult(
  val response: String,
  val usage: Usage?,
  var price: Int = 0,
) {
  data class DiagnosticInfo(
    val request: Any,
    var response: Any,
  )

  data class Usage(
    val inputTokens: Long? = null,
    val outputTokens: Long? = null,
    val cachedTokens: Long? = null,
  )
}
