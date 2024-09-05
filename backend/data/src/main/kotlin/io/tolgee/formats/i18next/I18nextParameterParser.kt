package io.tolgee.formats.i18next

class I18nextParameterParser {
  fun parse(match: MatchResult): ParsedI18nextParam? {
    return ParsedI18nextParam(
      key = match.groups.getGroupOrNull("key")?.value,
      nestedKey = match.groups.getGroupOrNull("nestedKey")?.value,
      format = match.groups.getGroupOrNull("format")?.value,
      fullMatch = match.value,
    )
  }

//  FIXME: move somewhere shared
  private fun MatchGroupCollection.getGroupOrNull(name: String): MatchGroup? {
    try {
      return this[name]
    } catch (e: IllegalArgumentException) {
      if (e.message?.contains("No group with name") != true) {
        throw e
      }
      return null
    }
  }
}
