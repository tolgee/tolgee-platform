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

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.UserAccount
import io.tolgee.model.oauth2.OAuth2Grant
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OAuth2AuthorizationServiceTest : AbstractSpringTest() {
  @Autowired
  private lateinit var repository: OAuth2GrantRepository

  @Autowired
  private lateinit var authorizationService: OAuth2AuthorizationService

  private lateinit var testData: BaseTestData
  private lateinit var userA: UserAccount
  private lateinit var userB: UserAccount

  @BeforeEach
  fun setup() {
    testData = BaseTestData()
    userA = testData.root.addUserAccount { username = "oauth_query_user_a" }.self
    userB = testData.root.addUserAccount { username = "oauth_query_user_b" }.self
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `revokeAllForUser deletes only the requesting user's grants`() {
    // logout-everywhere / password change deletes by user with no client filter; a wrong or missing WHERE clause here
    // would wipe every user's grants on any single user's invalidation.
    val a1 = insertGrant("client-x", userA)
    val a2 = insertGrant("client-y", userA)
    val b1 = insertGrant("client-x", userB)

    val deleted = authorizationService.revokeAllForUser(userA.id)

    deleted.assert.isEqualTo(2)
    repository.existsById(a1).assert.isFalse()
    repository.existsById(a2).assert.isFalse()
    repository.existsById(b1).assert.isTrue()
  }

  private fun insertGrant(
    clientId: String,
    user: UserAccount,
  ): Long {
    val grant =
      OAuth2Grant().apply {
        userAccount = user
        this.clientId = clientId
        redirectUri = "https://example.org/callback"
        codeChallenge = "challenge"
        requestedScopes = "translations.view"
      }
    repository.save(grant)
    return grant.id
  }
}
