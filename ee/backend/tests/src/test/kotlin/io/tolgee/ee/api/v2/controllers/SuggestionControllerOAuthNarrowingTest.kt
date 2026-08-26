package io.tolgee.ee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.SuggestionsTestData
import io.tolgee.fixtures.OAuth2TestTokens
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.SuggestionsMode
import io.tolgee.testing.AbstractControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository

/**
 * An OAuth token must not delete a suggestion through the own-author shortcut in
 * SuggestionController.checkCanDeleteSuggestion: a token whose scope∩project set excludes
 * translation-suggestions.manage stays bound by that scope even on suggestions its holder authored.
 */
class SuggestionControllerOAuthNarrowingTest : AbstractControllerTest() {
  @Autowired
  private lateinit var authorizationService: OAuth2AuthorizationService

  @Autowired
  private lateinit var registeredClientRepository: RegisteredClientRepository

  private lateinit var testData: SuggestionsTestData
  private lateinit var tokens: OAuth2TestTokens

  @BeforeEach
  fun setup() {
    testData = SuggestionsTestData(SuggestionsMode.ENABLED)
    testDataService.saveTestData(testData.root)
    tokens = OAuth2TestTokens(authorizationService, registeredClientRepository)
  }

  @AfterEach
  fun cleanup() {
    tokens.deleteAll()
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `the own-suggestion fallback cannot widen a token that lacks suggestions-manage`() {
    // projectTranslator authored czechSuggestions[0] and could delete it via the own-author shortcut. A token scoped
    // only to translations.view must not ride that shortcut — it needs translation-suggestions.manage like anyone else.
    val token = tokens.issue(subject = testData.projectTranslator.self.id, scopes = listOf("translations.view"))
    performDelete(suggestionUrl(testData.czechSuggestions[0].self.id), null, bearer(token)).andIsForbidden

    // Positive control: a token carrying the manage scope for a holder who has it can delete — proving the endpoint is
    // reachable and the guard above is what blocks, not an unreachable route.
    val moderatorToken =
      tokens.issue(
        subject = testData.suggestionModerator.self.id,
        scopes = listOf("translations.view", "translation-suggestions.manage"),
      )
    performDelete(suggestionUrl(testData.czechSuggestions[1].self.id), null, bearer(moderatorToken)).andIsOk
  }

  private fun suggestionUrl(suggestionId: Long) =
    "/v2/projects/${testData.project.id}/languages/${testData.czechLanguage.id}/" +
      "key/${testData.keys[0].self.id}/suggestion/$suggestionId"

  private fun bearer(token: String) = HttpHeaders().apply { add(HttpHeaders.AUTHORIZATION, "Bearer $token") }
}
