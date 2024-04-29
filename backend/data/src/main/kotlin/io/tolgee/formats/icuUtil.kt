package io.tolgee.formats

import io.tolgee.formats.escaping.ForceIcuEscaper

/**
 * When keeping the param as it is, we still need to escape it so it doesn't get interpreted as ICU syntax
 */
fun String.escapeIcu(isInPlural: Boolean): String {
  return ForceIcuEscaper(this, isInPlural).escaped
}
