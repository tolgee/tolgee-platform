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

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.oauth2.cimd")
class OAuth2CimdProperties {
  var enabled: Boolean = false

  /** Hosts allowed as CIMD client-id URLs; empty = fail-closed (resolves nothing even when [enabled]). Primary bound on the fetch. */
  var allowedHosts: List<String> = listOf()

  var fetchTimeoutMs: Long = 2000

  var maxDocumentBytes: Long = 32 * 1024

  var cacheTtlSeconds: Long = 300
}
