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

class OAuth2Error(
  val error: String,
  val description: String? = null,
) : RuntimeException(description?.let { "$error: $it" } ?: error) {
  companion object {
    const val INVALID_REQUEST = "invalid_request"
    const val INVALID_CLIENT = "invalid_client"
    const val INVALID_GRANT = "invalid_grant"
    const val INVALID_SCOPE = "invalid_scope"
    const val ACCESS_DENIED = "access_denied"
    const val UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type"
    const val UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type"
  }
}
