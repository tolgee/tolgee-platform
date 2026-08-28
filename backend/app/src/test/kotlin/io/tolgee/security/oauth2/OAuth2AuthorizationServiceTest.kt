package io.tolgee.security.oauth2

import io.tolgee.AbstractSpringTest
import io.tolgee.development.testDataBuilder.data.BaseTestData
import io.tolgee.model.UserAccount
import io.tolgee.model.oauth2.OAuth2Authorization
import io.tolgee.repository.oauth2.OAuth2AuthorizationRepository
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OAuth2AuthorizationServiceTest : AbstractSpringTest() {
  @Autowired
  private lateinit var repository: OAuth2AuthorizationRepository

  @Autowired
  private lateinit var queryService: OAuth2AuthorizationService

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
    queryService.revokeAllForUser(userA.id)
    queryService.revokeAllForUser(userB.id)
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `revokeAllForUser deletes only the requesting user's grants`() {
    // logout-everywhere / password change deletes by user with no client filter; a wrong or missing WHERE clause here
    // would wipe every user's grants on any single user's invalidation.
    val a1 = insertGrant("client-x", userA)
    val a2 = insertGrant("client-y", userA)
    val b1 = insertGrant("client-x", userB)

    val deleted = queryService.revokeAllForUser(userA.id)

    deleted.assert.isEqualTo(2)
    repository.existsById(a1).assert.isFalse()
    repository.existsById(a2).assert.isFalse()
    repository.existsById(b1).assert.isTrue()
  }

  private fun insertGrant(
    clientId: String,
    user: UserAccount,
  ): Long {
    val authorization =
      OAuth2Authorization().apply {
        userAccount = user
        this.clientId = clientId
        redirectUri = "https://example.org/callback"
        codeChallenge = "challenge"
        requestedScopes = "translations.view"
      }
    repository.save(authorization)
    return authorization.id
  }
}
