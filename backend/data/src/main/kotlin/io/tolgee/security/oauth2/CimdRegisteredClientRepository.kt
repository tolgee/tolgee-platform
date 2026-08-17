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
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
@Primary
class CimdRegisteredClientRepository(
  @Qualifier("jdbcRegisteredClientRepository")
  private val jdbc: JdbcRegisteredClientRepository,
  private val fetcher: CimdMetadataFetcher,
  private val cimdProperties: OAuth2CimdProperties,
  private val currentDateProvider: CurrentDateProvider,
) : RegisteredClientRepository {
  private val byClientId = ConcurrentHashMap<String, CacheEntry>()
  private val idToClientId = ConcurrentHashMap<String, String>()

  override fun save(registeredClient: RegisteredClient) {
    jdbc.save(registeredClient)
  }

  override fun findById(id: String): RegisteredClient? {
    jdbc.findById(id)?.let { return it }
    val clientId = idToClientId[id] ?: return null
    return findByClientId(clientId)
  }

  override fun findByClientId(clientId: String): RegisteredClient? {
    jdbc.findByClientId(clientId)?.let { return it }
    if (!isUrlForm(clientId)) return null

    cached(clientId)?.let { return it.client }

    val resolved = fetcher.fetchAndValidate(clientId)
    put(clientId, resolved)
    return resolved
  }

  private fun isUrlForm(clientId: String): Boolean {
    return clientId.startsWith("https://")
  }

  private fun cached(clientId: String): CacheEntry? {
    val entry = byClientId[clientId] ?: return null
    if (entry.expiresAtMillis < now()) {
      byClientId.remove(clientId)
      entry.client?.let { idToClientId.remove(it.id) }
      return null
    }
    return entry
  }

  private fun put(
    clientId: String,
    client: RegisteredClient?,
  ) {
    if (byClientId.size >= MAX_CACHE_ENTRIES) pruneExpired()
    if (byClientId.size >= MAX_CACHE_ENTRIES) return
    val ttlSeconds = cacheTtlFor(client)
    val previous = byClientId.put(clientId, CacheEntry(client, now() + ttlSeconds * 1000))
    // deterministicId hashes the redirect uris, so a redirect change remaps this clientId to a new client.id; drop the
    // superseded reverse-mapping entry (and any negative entry has no id) so idToClientId can't grow without bound.
    previous?.client?.let { if (it.id != client?.id) idToClientId.remove(it.id) }
    client?.let { idToClientId[it.id] = clientId }
  }

  private fun cacheTtlFor(client: RegisteredClient?): Long {
    if (client == null) return NEGATIVE_CACHE_TTL_SECONDS
    return cimdProperties.cacheTtlSeconds
  }

  private fun pruneExpired() {
    val cutoff = now()
    byClientId.entries.removeIf { (_, entry) ->
      val expired = entry.expiresAtMillis < cutoff
      if (expired) entry.client?.let { idToClientId.remove(it.id) }
      expired
    }
  }

  private fun now(): Long = currentDateProvider.date.time

  private class CacheEntry(
    val client: RegisteredClient?,
    val expiresAtMillis: Long,
  )

  companion object {
    private const val MAX_CACHE_ENTRIES = 1000
    private const val NEGATIVE_CACHE_TTL_SECONDS = 60L
  }
}
