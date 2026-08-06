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

/**
 * A connected app is managed per client (revoking it drops all of that client's authorizations for the user). A
 * token's project narrowing (tg.prj) is a mint-time property enforced per request, not an independently revocable
 * grant — bindings to different projects for the same client are not surfaced or revoked separately.
 */
data class ConnectedAppModel(
  val clientId: String,
  val clientName: String,
  val scopes: List<String>,
  val lastAuthorizedAt: Long?,
)
