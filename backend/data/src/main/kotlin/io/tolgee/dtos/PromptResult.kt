package io.tolgee.dtos

import com.fasterxml.jackson.databind.JsonNode
import io.tolgee.dtos.response.prompt.PromptResponseUsageDto

class PromptResult(
  val response: String,
  val usage: PromptResponseUsageDto?,
  var parsedJson: JsonNode? = null,
  var price: Int = 0,
)
