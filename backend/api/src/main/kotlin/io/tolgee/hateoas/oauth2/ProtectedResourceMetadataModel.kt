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

package io.tolgee.hateoas.oauth2

import com.fasterxml.jackson.annotation.JsonProperty

data class ProtectedResourceMetadataModel(
  @get:JsonProperty("resource")
  val resource: String,
  @get:JsonProperty("authorization_servers")
  val authorizationServers: List<String>,
  @get:JsonProperty("scopes_supported")
  val scopesSupported: List<String>,
  @get:JsonProperty("bearer_methods_supported")
  val bearerMethodsSupported: List<String>,
)
