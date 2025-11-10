package io.tolgee.formats.i18next.`in`

import io.tolgee.formats.getGroupOrNull
import io.tolgee.formats.importCommon.ParsedPluralsKey
import io.tolgee.formats.importCommon.PluralsKeyParser

class PluralsI18nextKeyParser(
  private val keyRegex: Regex,
) : PluralsKeyParser {
  override fun parse(key: String): ParsedPluralsKey {
    val match = keyRegex.find(key)
    return ParsedPluralsKey(
      key = match?.groups?.getGroupOrNull("key")?.value,
      plural = match?.groups?.getGroupOrNull("plural")?.value,
      originalKey = key,
    )
  }
}
