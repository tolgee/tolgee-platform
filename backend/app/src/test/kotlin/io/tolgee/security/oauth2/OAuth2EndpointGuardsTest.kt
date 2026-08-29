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

import io.tolgee.api.v2.controllers.oauth2.OAuth2FlowController
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.BypassEmailVerification
import io.tolgee.security.authentication.BypassForcedSsoAuthentication
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

/**
 * The consent API is guarded by what it does *not* declare: absence of [AllowApiAccess] is what keeps API credentials
 * out, and absence of the two bypasses is what keeps an unverified-email or forced-SSO user out. Nothing else in the
 * codebase would fail if one of them were added back, so it is asserted here directly.
 */
class OAuth2EndpointGuardsTest {
  @Test
  fun `no consent endpoint opts out of authentication policy`() {
    val optedOut =
      OAuth2FlowController::class.java.declaredMethods
        .filter { method ->
          FORBIDDEN_ANNOTATIONS.any { method.isAnnotationPresent(it) }
        }.map { it.name }

    optedOut.assert.isEmpty()
  }

  companion object {
    private val FORBIDDEN_ANNOTATIONS =
      listOf(
        AllowApiAccess::class.java,
        BypassEmailVerification::class.java,
        BypassForcedSsoAuthentication::class.java,
      )
  }
}
