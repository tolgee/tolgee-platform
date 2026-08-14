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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import java.util.Date

/**
 * The @Primary repository's own routing/caching: only https client ids fall back to the fetcher, resolved clients are
 * cached for the TTL and re-fetched after it, and the cache is bounded so many distinct CIMD urls can't grow it forever.
 */
class CimdRegisteredClientRepositoryTest {
  private val jdbc = mock<JdbcRegisteredClientRepository> { on { findByClientId(any()) } doReturn null }
  private val fetcher = mock<CimdMetadataFetcher>()
  private val properties = OAuth2CimdProperties().apply { cacheTtlSeconds = 300 }
  private var now = 1_000_000L
  private val dateProvider = mock<CurrentDateProvider> { on { date } doReturn Date(now) }

  private fun repo() = CimdRegisteredClientRepository(jdbc, fetcher, properties, dateProvider)

  private fun client(clientId: String): RegisteredClient =
    RegisteredClient
      .withId("cimd_$clientId")
      .clientId(clientId)
      .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
      .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
      .redirectUri("https://example.com/cb")
      .build()

  private fun advanceClock(seconds: Long) {
    now += seconds * 1000
    whenever(dateProvider.date).thenReturn(Date(now))
  }

  @Test
  fun `a non-https unknown client id returns null without touching the fetcher`() {
    assertThat(repo().findByClientId("not-a-url")).isNull()
    verify(fetcher, never()).fetchAndValidate(any())
  }

  @Test
  fun `an https client id is fetched once and then served from cache`() {
    whenever(fetcher.fetchAndValidate("https://example.com/client")).thenReturn(client("https://example.com/client"))
    val repo = repo()

    assertThat(repo.findByClientId("https://example.com/client")).isNotNull
    assertThat(repo.findByClientId("https://example.com/client")).isNotNull
    verify(fetcher, times(1)).fetchAndValidate("https://example.com/client")
  }

  @Test
  fun `an expired cache entry is re-fetched, not served stale`() {
    whenever(fetcher.fetchAndValidate("https://example.com/client")).thenReturn(client("https://example.com/client"))
    val repo = repo()

    repo.findByClientId("https://example.com/client")
    advanceClock(properties.cacheTtlSeconds + 1)
    repo.findByClientId("https://example.com/client")

    verify(fetcher, times(2)).fetchAndValidate("https://example.com/client")
  }

  @Test
  fun `findById resolves a cached CIMD client by its generated id and returns null for an unknown id`() {
    val resolved = client("https://example.com/client")
    whenever(fetcher.fetchAndValidate("https://example.com/client")).thenReturn(resolved)
    val repo = repo()

    repo.findByClientId("https://example.com/client")
    assertThat(repo.findById(resolved.id)?.clientId).isEqualTo("https://example.com/client")
    verify(fetcher, times(1)).fetchAndValidate("https://example.com/client")
    assertThat(repo.findById("cimd_unknown")).isNull()
  }

  @Test
  fun `a failed lookup is cached so an unresolvable client id does not re-fetch on every request`() {
    whenever(fetcher.fetchAndValidate("https://example.com/unknown")).thenReturn(null)
    val repo = repo()

    assertThat(repo.findByClientId("https://example.com/unknown")).isNull()
    assertThat(repo.findByClientId("https://example.com/unknown")).isNull()
    verify(fetcher, times(1)).fetchAndValidate("https://example.com/unknown")
  }

  @Test
  fun `a cached failed lookup is retried after the short negative TTL`() {
    whenever(fetcher.fetchAndValidate("https://example.com/unknown")).thenReturn(null)
    val repo = repo()

    repo.findByClientId("https://example.com/unknown")
    advanceClock(61)
    repo.findByClientId("https://example.com/unknown")
    verify(fetcher, times(2)).fetchAndValidate("https://example.com/unknown")
  }

  @Test
  fun `the resolved-client cache is bounded so distinct urls beyond the cap are not cached`() {
    whenever(fetcher.fetchAndValidate(any())).thenAnswer { client(it.getArgument(0)) }
    val repo = repo()

    repeat(1000) { repo.findByClientId("https://example.com/c$it") }
    val overflow = "https://example.com/overflow"

    repo.findByClientId(overflow)
    repo.findByClientId(overflow)
    verify(fetcher, times(2)).fetchAndValidate(overflow)
  }
}
