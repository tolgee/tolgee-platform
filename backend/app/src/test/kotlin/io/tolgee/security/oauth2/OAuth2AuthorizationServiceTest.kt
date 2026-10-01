package io.tolgee.security.oauth2

import io.tolgee.AbstractSpringTest
import io.tolgee.component.KeyGenerator
import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.development.testDataBuilder.data.OAuth2AuthorizationServiceTestData
import io.tolgee.development.testDataBuilder.newOAuth2Grant
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import io.tolgee.model.oauth2.OAuth2Grant
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.security.oauth2.cimd.CimdMetadataFetcher
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.util.Date

class OAuth2AuthorizationServiceTest : AbstractSpringTest() {
  @Autowired
  private lateinit var repository: OAuth2GrantRepository

  @Autowired
  private lateinit var authorizationService: OAuth2AuthorizationService

  @Autowired
  private lateinit var keyGenerator: KeyGenerator

  @Autowired
  private lateinit var properties: OAuth2ServerProperties

  private lateinit var testData: OAuth2AuthorizationServiceTestData

  @BeforeEach
  fun setup() {
    testData = OAuth2AuthorizationServiceTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
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
        codeChallenge = OAuth2Pkce.s256(verifier)
        codeExpiresAt = Date.from(currentDateProvider.date.toInstant().plusSeconds(300))
        bindProjects(null)
        maxGrantedScopeValues = listOf(Scope.TRANSLATIONS_VIEW.value)
        issuedTokenScopeValues = listOf(Scope.TRANSLATIONS_VIEW.value)
      }
    repository.save(grant)

    assertThrows<OAuth2Error> {
      authorizationService.exchangeCode(client, CODE, grant.redirectUri, "b".repeat(43), null)
    }

    assertThrows<OAuth2Error> { authorizationService.exchangeCode(client, CODE, grant.redirectUri, verifier, null) }
    repository.existsById(grant.id).assert.isFalse()
  }

  @Test
  fun `a refresh from two generations back kills the grant via the history`() {
    val grant = grantWithRefreshChain()
    val first = authorizationService.refresh(client, "tgort_$OLDEST", null, null)
    authorizationService.refresh(client, first.refreshToken, null, null)
    currentDateProvider.move(Duration.ofSeconds(properties.refreshTokenGraceSeconds + 5))

    assertThrows<OAuth2Error> { authorizationService.refresh(client, "tgort_$OLDEST", null, null) }
    repository.existsById(grant.id).assert.isFalse()
  }

  @Test
  fun `a code minted against a since-changed CIMD document is refused and the grant killed`() {
    val grant = cimdGrant(V1 + "hash-at-authorize-time")

    val changedClient = cimdClient(V1 + "hash-after-the-document-changed")
    assertThrows<OAuth2Error> {
      authorizationService.exchangeCode(changedClient, CODE, grant.redirectUri, VERIFIER, null)
    }
    repository.existsById(grant.id).assert.isFalse()
  }

  @Test
  fun `a code exchange with the unchanged CIMD document succeeds`() {
    val grant = cimdGrant(V1 + "stable-hash")

    val sameClient = cimdClient(V1 + "stable-hash")
    val tokens = authorizationService.exchangeCode(sameClient, CODE, grant.redirectUri, VERIFIER, null)
    tokens.accessToken.assert.isNotBlank()
  }

  @Test
  fun `a client with no hash cannot assert drift, so an unresolvable document leaves the grant alone`() {
    val grant = cimdGrant(V1 + "hash-at-authorize-time")

    val unresolvedClient = cimdClient(metadataHash = null, redirectUris = emptyList())
    val tokens = authorizationService.exchangeCode(unresolvedClient, CODE, grant.redirectUri, VERIFIER, null)

    tokens.accessToken.assert.isNotBlank()
    repository.existsById(grant.id).assert.isTrue()
  }

  @Test
  fun `a hash whose scheme this build cannot reproduce is not read as drift`() {
    val grant = cimdGrant("hash-from-a-release-that-did-not-version-it")

    val currentClient = cimdClient(V1 + "whatever-this-build-computes")
    authorizationService.exchangeCode(currentClient, CODE, grant.redirectUri, VERIFIER, null)

    repository.existsById(grant.id).assert.isTrue()
  }

  private val client =
    OAuth2Client(
      clientId = OAuth2Constants.BROWSER_EXTENSION_CLIENT_ID,
      name = "Test client",
      redirectUris = listOf("https://example.org/callback"),
    )

  private fun cimdGrant(metadataHash: String?): OAuth2Grant =
    repository.save(
      newOAuth2Grant(testData.userA, clientId = CIMD_URL).apply {
        redirectUri = CIMD_REDIRECT
        codeHash = keyGenerator.hash(CODE)
        codeChallenge = OAuth2Pkce.s256(VERIFIER)
        codeExpiresAt = Date.from(currentDateProvider.date.toInstant().plusSeconds(300))
        clientMetadataHash = metadataHash
        bindProjects(null)
        maxGrantedScopeValues = listOf(Scope.TRANSLATIONS_VIEW.value)
        issuedTokenScopeValues = listOf(Scope.TRANSLATIONS_VIEW.value)
      },
    )

  private fun cimdClient(
    metadataHash: String?,
    redirectUris: List<String> = listOf(CIMD_REDIRECT),
  ) = OAuth2Client(CIMD_URL, CIMD_URL, redirectUris, verified = false, metadataHash = metadataHash)

  private fun grantWithRefreshChain(): OAuth2Grant {
    val grant =
      newOAuth2Grant(testData.userA).apply {
        refreshTokenHash = keyGenerator.hash(OLDEST)
        refreshTokenExpiresAt = Date.from(currentDateProvider.date.toInstant().plusSeconds(3600))
        maxGrantedScopeValues = listOf(Scope.TRANSLATIONS_VIEW.value)
        issuedTokenScopeValues = listOf(Scope.TRANSLATIONS_VIEW.value)
        bindProjects(null)
      }
    return repository.save(grant)
  }

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
    private const val CIMD_URL = "https://app.example.com/.well-known/oauth-client"
    private const val CIMD_REDIRECT = "https://app.example.com/callback"
    private val VERIFIER = "a".repeat(43)
    private const val V1 = CimdMetadataFetcher.HASH_SCHEME_PREFIX
    private const val OLDEST = "oldest-refresh-token-secret"
  }
}
