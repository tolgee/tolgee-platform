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

/** @Primary repo: fixed clients from the JDBC store; an unknown URL-form `client_id` falls back to CIMD (transient, TTL-cached). */
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

    cached(clientId)?.let { return it }

    val resolved = fetcher.fetchAndValidate(clientId) ?: return null
    put(clientId, resolved)
    return resolved
  }

  private fun isUrlForm(clientId: String): Boolean {
    return clientId.startsWith("https://")
  }

  private fun cached(clientId: String): RegisteredClient? {
    val entry = byClientId[clientId] ?: return null
    if (entry.expiresAtMillis < now()) {
      byClientId.remove(clientId)
      idToClientId.remove(entry.client.id)
      return null
    }
    return entry.client
  }

  private fun put(
    clientId: String,
    client: RegisteredClient,
  ) {
    if (byClientId.size >= MAX_CACHE_ENTRIES) pruneExpired()
    if (byClientId.size >= MAX_CACHE_ENTRIES) return
    byClientId[clientId] = CacheEntry(client, now() + cimdProperties.cacheTtlSeconds * 1000)
    idToClientId[client.id] = clientId
  }

  private fun pruneExpired() {
    val cutoff = now()
    byClientId.entries.removeIf { (_, entry) ->
      val expired = entry.expiresAtMillis < cutoff
      if (expired) idToClientId.remove(entry.client.id)
      expired
    }
  }

  private fun now(): Long = currentDateProvider.date.time

  private class CacheEntry(
    val client: RegisteredClient,
    val expiresAtMillis: Long,
  )

  companion object {
    private const val MAX_CACHE_ENTRIES = 1000
  }
}
