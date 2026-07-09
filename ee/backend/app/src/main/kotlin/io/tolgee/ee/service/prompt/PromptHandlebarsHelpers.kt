package io.tolgee.ee.service.prompt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper

object PromptHandlebarsHelpers {
  const val ESCAPE_JSON = "escapeJson"

  private val objectMapper = jacksonObjectMapper()

  /**
   * Marks a value that already went through JSON escaping, so [escapeJson] skips it. Some
   * variables (`source.translation` and friends) are escaped when collected, and escaping them
   * a second time would emit `\\"` where the template author expects `\"`.
   */
  class JsonEscapedString(
    escaped: String,
  ) : Handlebars.SafeString(escaped)

  fun register(handlebars: Handlebars): Handlebars =
    handlebars.registerHelper(
      ESCAPE_JSON,
      Helper<Any?> { value, _ -> escapeJson(value) },
    )

  fun escapeJson(value: Any?): JsonEscapedString {
    if (value is JsonEscapedString) return value
    return JsonEscapedString(escapeJsonString(value))
  }

  private fun escapeJsonString(value: Any?): String {
    if (value == null) return ""
    return objectMapper.writeValueAsString(value.toString()).removeSurrounding("\"")
  }
}
