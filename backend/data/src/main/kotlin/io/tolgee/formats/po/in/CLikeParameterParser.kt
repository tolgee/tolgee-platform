package io.tolgee.formats.po.`in`

class CLikeParameterParser {
  fun parse(match: MatchResult): ParsedCLikeParam? {
    val specifierGroup = match.groups["specifier"]
    val specifier = specifierGroup?.value

    return ParsedCLikeParam(
      argNum = match.groups.getGroupOrNull("argnum")?.value,
      argName =
        match.groups.getGroupOrNull("argname")?.value
          ?: match.groups.getGroupOrNull("argname2")?.value,
      width = match.groups.getGroupOrNull("width")?.value?.toIntOrNull(),
      precision = match.groups.getGroupOrNull("precision")?.value?.toInt(),
      length = match.groups.getGroupOrNull("length")?.value,
      specifier = specifier,
      flags = match.groups.getGroupOrNull("flags")?.value,
      fullMatch = match.value,
    )
  }

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
