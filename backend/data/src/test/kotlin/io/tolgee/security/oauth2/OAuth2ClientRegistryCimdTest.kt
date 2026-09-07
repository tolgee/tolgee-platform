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

import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.OAuth2CimdProperties
import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.time.Instant
import java.util.Date

class OAuth2ClientRegistryCimdTest {
  private val clientIdUrl = "https://client.example/metadata"
  private val cimdClient =
    CimdClient(
      client =
        OAuth2Client(
          clientId = clientIdUrl,
          name = "Example",
          redirectUris = listOf("https://client.example/cb"),
        ),
      logoUri = null,
      metadataHash = "hash",
    )

  private var now = Date.from(Instant.parse("2026-08-07T00:00:00Z"))
  private val currentDateProvider = mock<CurrentDateProvider> { on { date } doAnswer { now } }
  private val cimdProperties =
    OAuth2CimdProperties().apply {
      cacheTtlSeconds = 300
      negativeCacheTtlSeconds = 60
    }

  private val issuerResolver = mock<OAuth2IssuerResolver> { on { issuerUrl } doReturn "https://tolgee.example.com" }

  @Test
  fun `a url-form client id resolves through the fetcher`() {
    val fetcher = mock<CimdMetadataFetcher> { on { fetchAndValidate(clientIdUrl) } doReturn cimdClient }
    val registry = registry(fetcher)

    registry.find(clientIdUrl).assert.isEqualTo(cimdClient.client)

    verify(fetcher).fetchAndValidate(clientIdUrl)
  }

  @Test
  fun `a non-url client id never touches the fetcher`() {
    val fetcher = mock<CimdMetadataFetcher>()
    val registry = registry(fetcher)

    registry.find("not-a-url-client-id").assert.isNull()

    verifyNoInteractions(fetcher)
  }

  @Test
  fun `a resolved client is cached until the ttl passes`() {
    val fetcher = mock<CimdMetadataFetcher> { on { fetchAndValidate(clientIdUrl) } doReturn cimdClient }
    val registry = registry(fetcher)

    registry.find(clientIdUrl)
    registry.find(clientIdUrl)
    now = Date(now.time + (cimdProperties.cacheTtlSeconds + 1) * 1000)
    registry.find(clientIdUrl)

    verify(fetcher, times(2)).fetchAndValidate(clientIdUrl)
  }

  @Test
  fun `a failed resolution is negatively cached`() {
    val fetcher = mock<CimdMetadataFetcher> { on { fetchAndValidate(clientIdUrl) } doReturn null }
    val registry = registry(fetcher)

    registry.find(clientIdUrl)
    registry.find(clientIdUrl)

    verify(fetcher, times(1)).fetchAndValidate(clientIdUrl)
  }

  @Test
  fun `isStillAuthorized is true for a cached cimd client and does not re-fetch`() {
    val fetcher = mock<CimdMetadataFetcher> { on { fetchAndValidate(clientIdUrl) } doReturn cimdClient }
    val registry = registry(fetcher)
    registry.find(clientIdUrl)

    registry.isStillAuthorized(clientIdUrl).assert.isTrue()

    verify(fetcher, times(1)).fetchAndValidate(clientIdUrl)
  }

  @Test
  fun `isEnabled is true with no pre-registered clients when the issuer resolves`() {
    val registry = registry(mock())

    registry.clients.assert.isEmpty()
    registry.isEnabled.assert.isTrue()
  }

  private fun registry(fetcher: CimdMetadataFetcher): OAuth2ClientRegistry =
    OAuth2ClientRegistry(
      OAuth2ServerProperties(),
      fetcher,
      cimdProperties,
      currentDateProvider,
      issuerResolver,
    )
}
