package io.tolgee.formats.i18next.`in`

import io.tolgee.formats.getGroupOrNull

class I18nextParameterParser {
  fun parse(match: MatchResult): ParsedI18nextParam? {
    return ParsedI18nextParam(
      key = match.groups.getGroupOrNull("key")?.value,
      nestedKey = match.groups.getGroupOrNull("nestedKey")?.value,
      format = match.groups.getGroupOrNull("format")?.value,
      keepUnescaped = (match.groups.getGroupOrNull("unescapedflag")?.value?.length ?: 0) > 0,
      fullMatch = match.value,
    )
  }
}
