package io.tolgee.formats.i18next

class PluralsI18nextKeyParser {
  fun parse(match: MatchResult): ParsedI18nextKey? {
    return ParsedI18nextKey(
      key = match.groups.getGroupOrNull("key")?.value,
      plural = match.groups.getGroupOrNull("plural")?.value,
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
