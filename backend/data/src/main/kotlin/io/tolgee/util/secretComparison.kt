package io.tolgee.util

import java.security.MessageDigest

/** Compares two secrets without leaking their common prefix length through timing. */
fun constantTimeEquals(
  a: String,
  b: String,
): Boolean {
  return MessageDigest.isEqual(a.toByteArray(), b.toByteArray())
}
