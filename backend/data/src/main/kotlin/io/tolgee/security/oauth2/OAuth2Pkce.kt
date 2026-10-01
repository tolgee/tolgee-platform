/**
 * Copyright (C) 2026 Tolgee s.r.o. and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.tolgee.security.oauth2

import java.security.MessageDigest
import java.util.Base64

object OAuth2Pkce {
  /** RFC 7636 §4.1: 43-128 characters of unreserved ASCII. */
  fun isValidCodeVerifier(verifier: String): Boolean =
    verifier.length in 43..128 && verifier.all { it in PKCE_UNRESERVED }

  /** RFC 7636 §4.2: an S256 challenge is the base64url-without-padding SHA-256 digest, i.e. exactly 43 such chars. */
  fun isValidCodeChallenge(challenge: String): Boolean =
    challenge.length == 43 && challenge.all { it in PKCE_UNRESERVED }

  fun matchesChallenge(
    verifier: String,
    challenge: String,
  ): Boolean = constantTimeEquals(s256(verifier), challenge)

  fun s256(verifier: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
    return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
  }

  private fun constantTimeEquals(
    a: String,
    b: String,
  ): Boolean = MessageDigest.isEqual(a.toByteArray(Charsets.US_ASCII), b.toByteArray(Charsets.US_ASCII))

  private val PKCE_UNRESERVED =
    (('A'..'Z') + ('a'..'z') + ('0'..'9') + listOf('-', '.', '_', '~')).toSet()
}
