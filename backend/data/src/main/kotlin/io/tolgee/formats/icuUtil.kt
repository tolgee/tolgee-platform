package io.tolgee.formats

/**
 * When keeping the param as it is, we still need to escape it so it doesn't get interpreted as ICU syntax
 */
fun String.escapeIcu(isInPlural: Boolean): String {
  return IcuMessageEscaper(this, isInPlural).escaped
}
