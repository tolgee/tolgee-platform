package io.tolgee.util

import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.Base64

/** The SHA-256 digest of [value], base64url-encoded without padding: the form PKCE (RFC 7636 §4.2) spells out. */
fun sha256Base64Url(
  value: String,
  charset: Charset = Charsets.UTF_8,
): String {
  val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(charset))
  return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
}
