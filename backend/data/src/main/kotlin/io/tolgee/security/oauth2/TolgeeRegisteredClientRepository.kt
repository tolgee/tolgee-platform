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

import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * The OAuth clients this server accepts, built from configuration at startup.
 *
 * Configuration is the only source: nothing registers a client at runtime, and [save] holds one for this replica's
 * lifetime only — Tolgee exposes no dynamic client registration, so SAS never calls it outside tests.
 *
 * Not Spring's `InMemoryRegisteredClientRepository`: its constructor rejects an empty list, and no clients configured
 * is the default state.
 */
@Component
class TolgeeRegisteredClientRepository(
  private val preRegisteredClients: PreRegisteredClients,
) : RegisteredClientRepository {
  private val byId = ConcurrentHashMap<String, RegisteredClient>()
  private val byClientId = ConcurrentHashMap<String, RegisteredClient>()

  init {
    resetToConfigured()
  }

  override fun save(registeredClient: RegisteredClient) = put(registeredClient)

  /**
   * Restores exactly the configured set, dropping everything [save] added.
   *
   * Clients live here rather than in the database, so a test that registers its own is not undone by the per-test
   * database reset; without this they would stay visible to every later test sharing the application context.
   */
  fun resetToConfigured() {
    byId.clear()
    byClientId.clear()
    preRegisteredClients.clients().forEach(::put)
  }

  override fun findById(id: String): RegisteredClient? = byId[id]

  override fun findByClientId(clientId: String): RegisteredClient? = byClientId[clientId]

  private fun put(client: RegisteredClient) {
    byId[client.id] = client
    byClientId[client.clientId] = client
  }
}
