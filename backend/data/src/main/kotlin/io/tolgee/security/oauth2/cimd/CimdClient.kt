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

package io.tolgee.security.oauth2.cimd

import io.tolgee.security.oauth2.OAuth2Client

/**
 * The result of resolving a Client ID Metadata Document: the unverified [OAuth2Client] it describes plus the logo the
 * document self-asserted (kept only when same-origin, else null). The consent screen renders the unverified treatment.
 */
data class CimdClient(
  val client: OAuth2Client,
  val logoUri: String?,
)
