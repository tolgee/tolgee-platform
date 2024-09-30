package io.tolgee.formats.paramConvertors.`in`

import io.tolgee.formats.ToIcuPlaceholderConvertor
import io.tolgee.formats.escapeIcu
import io.tolgee.formats.i18next.I18NEXT_UNESCAPED_FLAG_CUSTOM_KEY
import io.tolgee.formats.i18next.`in`.I18nextParameterParser
import io.tolgee.formats.i18next.`in`.ParsedI18nextParam
import io.tolgee.formats.i18next.`in`.PluralsI18nextKeyParser

class I18nextToIcuPlaceholderConvertor : ToIcuPlaceholderConvertor {
  private val parser = I18nextParameterParser()

  override val regex: Regex
    get() = I18NEXT_PLACEHOLDER_REGEX

  override val pluralArgName: String? = I18NEXT_PLURAL_ARG_NAME

  private var unescapedKeys = mutableListOf<String>()
  private var escapedKeys = mutableListOf<String>()

  override val customValuesModifier: (
    (MutableMap<String, Any?>, MutableMap<String, Any?>) -> Unit
  )? = modifier@{ customValues, memory ->
    if (unescapedKeys.isEmpty() && escapedKeys.isEmpty()) {
      // Optimization
      return@modifier
    }

    customValues.modifyList(I18NEXT_UNESCAPED_FLAG_CUSTOM_KEY) { allUnescapedKeys ->
      memory.modifyList(I18NEXT_UNESCAPED_FLAG_CUSTOM_KEY) { allEscapedKeys ->
        unescapedKeys.forEach { unescapedKey ->
          handleUnescapedModifier(allUnescapedKeys, allEscapedKeys, unescapedKey)
        }
        escapedKeys.forEach { escapedKey ->
          handleEscapedModifier(allUnescapedKeys, allEscapedKeys, escapedKey)
        }
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun MutableMap<String, Any?>.modifyList(
    key: String,
    modifier: (MutableList<Any?>) -> Unit,
  ) {
    val value = this[key] ?: mutableListOf<Any?>()
    val list = value as? MutableList<Any?> ?: return

    modifier(list)

    if (list.isEmpty()) {
      remove(key)
      return
    }
    this[key] = list
  }

  private fun handleUnescapedModifier(
    unescapedKeys: MutableList<Any?>,
    escapedKeys: MutableList<Any?>,
    unescapedKey: String,
  ) {
    if (unescapedKey in escapedKeys || unescapedKey in unescapedKeys) {
      return
    }
    unescapedKeys.add(unescapedKey)
  }

  private fun handleEscapedModifier(
    unescapedKeys: MutableList<Any?>,
    escapedKeys: MutableList<Any?>,
    escapedKey: String,
  ) {
    if (escapedKey in escapedKeys) {
      return
    }
    escapedKeys.add(escapedKey)

    if (escapedKey !in unescapedKeys) {
      return
    }
    unescapedKeys.remove(escapedKey)
  }

  private fun ParsedI18nextParam.applyUnescapedFlag() {
    if (key == null) {
      return
    }
    if (keepUnescaped) {
      unescapedKeys.add(key)
      return
    }
    escapedKeys.add(key)
  }

  override fun convert(
    matchResult: MatchResult,
    isInPlural: Boolean,
  ): String {
    val parsed = parser.parse(matchResult) ?: return matchResult.value.escapeIcu(isInPlural)

    if (parsed.nestedKey != null) {
      // Nested keys are not supported
      return matchResult.value.escapeIcu(isInPlural)
    }

    parsed.applyUnescapedFlag()

    if (isInPlural && parsed.key == I18NEXT_PLURAL_ARG_NAME && parsed.format == null) {
      return "#"
    }

    return when (parsed.format) {
      null -> "{${parsed.key}}"
      "number" -> "{${parsed.key}, number}"
      else -> matchResult.value.escapeIcu(isInPlural)
    }
  }

  companion object {
    val I18NEXT_PLACEHOLDER_REGEX =
      """
      (?x)
      (
        \{\{
        (?<unescapedflag>-\ *)?
        (?<key>\w+)(?:,\ *(?<format>[^}]+))?
        }}
        |
        \\${'$'}t\(
        (?<nestedKey>[^)]+)
        \)
      )
      """.trimIndent().toRegex()

    val I18NEXT_DETECTION_REGEX =
      """
      (?x)
      (^|\W+)
      (
        \{\{
        (?<unescapedflag>-\ *)?
        (?<key>\w+)(?:,\ *(?<format>[^}]+))?
        }}
        |
        \\${'$'}t\(
        (?<nestedKey>[^)]+)
        \)
      )
      """.trimIndent().toRegex()

    val I18NEXT_PLURAL_ARG_NAME = "count"

    val I18NEXT_PLURAL_SUFFIX_REGEX = """^(?<key>\w+)_(?<plural>\w+)$""".toRegex()

    val I18NEXT_PLURAL_SUFFIX_KEY_PARSER = PluralsI18nextKeyParser(I18NEXT_PLURAL_SUFFIX_REGEX)
  }
}
