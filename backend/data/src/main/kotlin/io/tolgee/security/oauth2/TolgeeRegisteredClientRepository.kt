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
 * Holds the OAuth2 clients built from configuration ([PreRegisteredClients]).
 *
 * Clients are derived entirely from properties, so there is nothing to persist: a restart rebuilds the same set, and
 * emptying a client's redirect config simply stops it from existing rather than needing a stored row deleted.
 *
 * Spring's own `InMemoryRegisteredClientRepository` cannot be used because its constructor rejects an empty list, and
 * no clients configured is the default state.
 */
@Component
class TolgeeRegisteredClientRepository(
  preRegisteredClients: PreRegisteredClients,
) : RegisteredClientRepository {
  private val byId = ConcurrentHashMap<String, RegisteredClient>()
  private val byClientId = ConcurrentHashMap<String, RegisteredClient>()

  init {
    preRegisteredClients.clients().forEach(::put)
  }

  override fun save(registeredClient: RegisteredClient) = put(registeredClient)

  override fun findById(id: String): RegisteredClient? = byId[id]

  override fun findByClientId(clientId: String): RegisteredClient? = byClientId[clientId]

  private fun put(client: RegisteredClient) {
    byId[client.id] = client
    byClientId[client.clientId] = client
  }
}
