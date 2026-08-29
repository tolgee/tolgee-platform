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

import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class OAuth2RedirectsTest {
  @Test
  fun `a space is percent-encoded, never written as a bare plus`() {
    OAuth2Redirects.encodeQueryValue("abc def").assert.isEqualTo("abc%20def")
    OAuth2Redirects.encodeQueryValue("abc+def").assert.isEqualTo("abc%2Bdef")
    OAuth2Redirects.encodeQueryValue("a b+c").assert.isEqualTo("a%20b%2Bc")
  }

  @Test
  fun `characters that would otherwise change the shape of the query are encoded`() {
    OAuth2Redirects.encodeQueryValue("a&b=c").assert.isEqualTo("a%26b%3Dc")
    OAuth2Redirects.encodeQueryValue("50%off").assert.isEqualTo("50%25off")
    OAuth2Redirects.encodeQueryValue("a#b").assert.isEqualTo("a%23b")
  }

  @Test
  fun `the code redirect appends to a redirect URI that already carries a query`() {
    val url = OAuth2Redirects.code("https://app.test/cb?next=1", "the-code", "https://tolgee.test", "st ate")

    url.assert.isEqualTo("https://app.test/cb?next=1&code=the-code&iss=https%3A%2F%2Ftolgee.test&state=st%20ate")
  }

  @Test
  fun `an error redirect omits the parameters it has no value for`() {
    val url =
      OAuth2Redirects.error(
        "https://app.test/cb",
        OAuth2Error(OAuth2Error.ACCESS_DENIED),
        "https://t.test",
        null,
      )

    url.assert.isEqualTo("https://app.test/cb?error=access_denied&iss=https%3A%2F%2Ft.test")
  }
}
