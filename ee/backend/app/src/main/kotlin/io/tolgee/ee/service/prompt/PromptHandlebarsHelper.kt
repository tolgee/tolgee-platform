package io.tolgee.ee.service.prompt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper

object PromptHandlebarsHelper {
  const val ESCAPE_JSON = "escapeJson"

  private val objectMapper = jacksonObjectMapper()

  /**
   * Marks a value that already went through JSON escaping, so [escapeJson] returns it as is. Some
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

  fun toRenderable(value: Any?): Any? {
    if (value is JsonEscapedString) return value
    if (value is String) return Handlebars.SafeString(value)
    return value
  }

  fun escapeJson(value: Any?): JsonEscapedString {
    if (value is JsonEscapedString) return value
    if (!isScalar(value)) return JsonEscapedString("")
    return JsonEscapedString(escapeJsonString(value))
  }

  /**
   * A bare `{{escapeJson}}` hands the helper the whole variable context. Stringifying that would
   * evaluate every lazy variable (glossary, translation memory, screenshots) and dump the map.
   */
  private fun isScalar(value: Any?): Boolean = value is CharSequence || value is Number || value is Boolean

  private fun escapeJsonString(value: Any?): String =
    objectMapper.writeValueAsString(value.toString()).removeSurrounding("\"")
}
