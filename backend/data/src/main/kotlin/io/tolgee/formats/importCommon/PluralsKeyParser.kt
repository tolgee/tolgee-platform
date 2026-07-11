package io.tolgee.formats.importCommon

interface PluralsKeyParser {
  fun parse(key: String): ParsedPluralsKey
}
