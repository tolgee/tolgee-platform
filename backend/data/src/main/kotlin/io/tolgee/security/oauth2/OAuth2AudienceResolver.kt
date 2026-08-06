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

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.stereotype.Component

/** The REST API audience string (mint and verify both read it from here). */
@Component
class OAuth2AudienceResolver(
  private val tolgeeProperties: TolgeeProperties,
) {
  val apiAudience: String
    get() = tolgeeProperties.backEndUrl ?: tolgeeProperties.frontEndUrl ?: DEFAULT_API_AUDIENCE

  companion object {
    const val DEFAULT_API_AUDIENCE = "tolgee-api"
  }
}
