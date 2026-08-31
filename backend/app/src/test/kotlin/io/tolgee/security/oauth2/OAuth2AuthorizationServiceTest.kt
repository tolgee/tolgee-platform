package io.tolgee.security.oauth2

import io.tolgee.AbstractSpringTest
import io.tolgee.component.KeyGenerator
import io.tolgee.development.testDataBuilder.data.OAuth2AuthorizationServiceTestData
import io.tolgee.development.testDataBuilder.newOAuth2Grant
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.security.MessageDigest
import java.util.Base64
import java.util.Date

class OAuth2AuthorizationServiceTest : AbstractSpringTest() {
  @Autowired
  private lateinit var repository: OAuth2GrantRepository

  @Autowired
  private lateinit var authorizationService: OAuth2AuthorizationService

  @Autowired
  private lateinit var keyGenerator: KeyGenerator

  private lateinit var testData: OAuth2AuthorizationServiceTestData

  @BeforeEach
  fun setup() {
    testData = OAuth2AuthorizationServiceTestData()
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
    val a1 = insertGrant("client-x", testData.userA)
    val a2 = insertGrant("client-y", testData.userA)
    val b1 = insertGrant("client-x", testData.userB)

    val deleted = authorizationService.revokeAllForUser(testData.userA.id)

    deleted.assert.isEqualTo(2)
    repository.existsById(a1).assert.isFalse()
    repository.existsById(a2).assert.isFalse()
    repository.existsById(b1).assert.isTrue()
  }

  @Test
  fun `a code exchange with the wrong verifier spends the code, so the right verifier no longer redeems it`() {
    val verifier = "a".repeat(43)
    val grant =
      newOAuth2Grant(testData.userA).apply {
        codeHash = keyGenerator.hash(CODE)
        codeChallenge = s256(verifier)
        codeExpiresAt = Date.from(currentDateProvider.date.toInstant().plusSeconds(300))
        bindProjects(null)
        maxGrantedScopeValues = listOf(Scope.TRANSLATIONS_VIEW.value)
        issuedTokenScopeValues = listOf(Scope.TRANSLATIONS_VIEW.value)
      }
    repository.save(grant)

    assertThrows<OAuth2Error> { authorizationService.exchangeCode(client, CODE, grant.redirectUri, "b".repeat(43)) }

    assertThrows<OAuth2Error> { authorizationService.exchangeCode(client, CODE, grant.redirectUri, verifier) }
    repository.existsById(grant.id).assert.isFalse()
  }

  private val client =
    OAuth2Client(
      clientId = OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID,
      name = "Test client",
      redirectUris = listOf("https://example.org/callback"),
    )

  private fun s256(verifier: String): String =
    Base64
      .getUrlEncoder()
      .withoutPadding()
      .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

  private fun insertGrant(
    clientId: String,
    user: UserAccount,
  ): Long {
    val grant = newOAuth2Grant(user, clientId)
    repository.save(grant)
    return grant.id
  }

  companion object {
    private const val CODE = "test-authorization-code"
  }
}
