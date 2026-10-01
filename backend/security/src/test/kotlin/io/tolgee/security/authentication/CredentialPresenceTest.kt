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

package io.tolgee.security.authentication

import io.tolgee.testing.assert
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CredentialPresenceTest {
  @Test
  fun `finds the api key in the query string`() {
    CredentialPresence.hasAny(requestWith(query = "ak=tgpak_x")).assert.isTrue()
    CredentialPresence.hasAny(requestWith(query = "foo=1&ak=tgpak_x&bar=2")).assert.isTrue()
    CredentialPresence.hasAny(requestWith(query = "ak")).assert.isTrue()
  }

  @Test
  fun `finds it under a percent-encoded name, which the servlet container decodes before the filter matches it`() {
    CredentialPresence.hasAny(requestWith(query = "%61k=tgpak_x")).assert.isTrue()
    CredentialPresence.hasAny(requestWith(query = "foo=1&%61%6B=tgpak_x")).assert.isTrue()
  }

  @Test
  fun `does not mistake another parameter for it`() {
    CredentialPresence.hasAny(requestWith(query = "make=1")).assert.isFalse()
    CredentialPresence.hasAny(requestWith(query = "aka=1")).assert.isFalse()
    CredentialPresence.hasAny(requestWith(query = null)).assert.isFalse()
  }

  @Test
  fun `finds the header forms`() {
    CredentialPresence.hasAny(requestWith(authorization = "Bearer x")).assert.isTrue()
    CredentialPresence.hasAny(requestWith(apiKeyHeader = "tgpak_x")).assert.isTrue()
  }

  private fun requestWith(
    query: String? = null,
    authorization: String? = null,
    apiKeyHeader: String? = null,
  ): HttpServletRequest {
    val request = mock<HttpServletRequest>()
    whenever(request.queryString).thenReturn(query)
    whenever(request.getHeader(CredentialPresence.AUTHORIZATION_HEADER)).thenReturn(authorization)
    whenever(request.getHeader(CredentialPresence.API_KEY_HEADER)).thenReturn(apiKeyHeader)
    return request
  }
}
