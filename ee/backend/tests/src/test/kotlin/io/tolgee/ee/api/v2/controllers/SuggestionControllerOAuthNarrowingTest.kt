package io.tolgee.ee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.SuggestionsTestData
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.SuggestionsMode
import io.tolgee.security.oauth2.OAuth2AudienceResolver
import io.tolgee.security.oauth2.OAuth2Constants
import io.tolgee.testing.AbstractControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * An OAuth token must not delete a suggestion through the own-author shortcut in
 * SuggestionController.checkCanDeleteSuggestion: a token whose scope∩project set excludes
 * translation-suggestions.manage stays bound by that scope even on suggestions its holder authored.
 */
class SuggestionControllerOAuthNarrowingTest : AbstractControllerTest() {
  @Autowired
  private lateinit var jwtEncoder: JwtEncoder

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  private lateinit var testData: SuggestionsTestData

  @BeforeEach
  fun setup() {
    testData = SuggestionsTestData(SuggestionsMode.ENABLED)
    testDataService.saveTestData(testData.root)
    // Tokens are minted directly (bypassing SAS); insert an authorization row the resolver's liveness check can find.
    jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE id = ?", LIVE_AUTHORIZATION_ID)
    jdbcTemplate.update(
      "INSERT INTO oauth2_authorization (id, registered_client_id, principal_name, authorization_grant_type) " +
        "VALUES (?, ?, ?, ?)",
      LIVE_AUTHORIZATION_ID,
      "test-client",
      "test",
      "authorization_code",
    )
  }

  @AfterEach
  fun cleanup() {
    jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE id = ?", LIVE_AUTHORIZATION_ID)
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `the own-suggestion fallback cannot widen a token that lacks suggestions-manage`() {
    // projectTranslator authored czechSuggestions[0] and could delete it via the own-author shortcut. A token scoped
    // only to translations.view must not ride that shortcut — it needs translation-suggestions.manage like anyone else.
    val token = mint(subject = testData.projectTranslator.self.id, scopes = listOf("translations.view"))
    performDelete(suggestionUrl(testData.czechSuggestions[0].self.id), null, bearer(token)).andIsForbidden

    // Positive control: a token carrying the manage scope for a holder who has it can delete — proving the endpoint is
    // reachable and the guard above is what blocks, not an unreachable route.
    val moderatorToken =
      mint(
        subject = testData.suggestionModerator.self.id,
        scopes = listOf("translations.view", "translation-suggestions.manage"),
      )
    performDelete(suggestionUrl(testData.czechSuggestions[1].self.id), null, bearer(moderatorToken)).andIsOk
  }

  private fun suggestionUrl(suggestionId: Long) =
    "/v2/projects/${testData.project.id}/languages/${testData.czechLanguage.id}/" +
      "key/${testData.keys[0].self.id}/suggestion/$suggestionId"

  private fun bearer(token: String) = HttpHeaders().apply { add(HttpHeaders.AUTHORIZATION, "Bearer $token") }

  private fun mint(
    subject: Long,
    scopes: List<String>,
  ): String {
    val now = Instant.now()
    val claims =
      JwtClaimsSet
        .builder()
        .subject(subject.toString())
        .audience(listOf(apiAudience))
        .issuedAt(now)
        .expiresAt(now.plus(30, ChronoUnit.MINUTES))
        .claim("scope", scopes)
        .claim(OAuth2Constants.PROJECTS_CLAIM, OAuth2Constants.ALL_PROJECTS)
        .claim(OAuth2Constants.AUTHORIZATION_ID_CLAIM, LIVE_AUTHORIZATION_ID)
        .build()
    val header = JwsHeader.with(SignatureAlgorithm.RS256).build()
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
  }

  private val apiAudience: String
    get() =
      tolgeeProperties.backEndUrl
        ?: tolgeeProperties.frontEndUrl
        ?: OAuth2AudienceResolver.DEFAULT_API_AUDIENCE

  companion object {
    private const val LIVE_AUTHORIZATION_ID = "suggestion-oauth-narrowing-test-authz"
  }
}
