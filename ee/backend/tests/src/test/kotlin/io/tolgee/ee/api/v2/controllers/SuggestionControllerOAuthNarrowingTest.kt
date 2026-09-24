package io.tolgee.ee.api.v2.controllers

import io.tolgee.component.KeyGenerator
import io.tolgee.development.testDataBuilder.data.SuggestionsTestData
import io.tolgee.fixtures.OAuth2TestTokens
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.bearerHeaders
import io.tolgee.model.enums.SuggestionsMode
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.testing.AbstractControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * Deleting a suggestion is bounded by the token, not by who wrote it: an OAuth token needs
 * translation-suggestions.own-access for its holder's own suggestion and translation-suggestions.manage for
 * anyone else's, whatever permissions the user behind it holds.
 */
class SuggestionControllerOAuthNarrowingTest : AbstractControllerTest() {
  @Autowired
  private lateinit var grantRepository: OAuth2GrantRepository

  @Autowired
  private lateinit var keyGenerator: KeyGenerator

  private lateinit var testData: SuggestionsTestData
  private lateinit var tokens: OAuth2TestTokens

  @BeforeEach
  fun setup() {
    testData = SuggestionsTestData(SuggestionsMode.ENABLED)
    testDataService.saveTestData(testData.root)
    tokens = OAuth2TestTokens(grantRepository, userAccountService, keyGenerator)
  }

  @AfterEach
  fun cleanup() {
    tokens.deleteAll()
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `a token without suggestions own-access cannot delete its holder's own suggestion`() {
    // projectTranslator authored czechSuggestions[0] and may delete it in the webapp, so this is the case where
    // being the author is not what decides.
    val token =
      tokens.issue(
        subject = testData.projectTranslator.self.id,
        scopes = listOf("translations.view"),
        projectIds = null,
      )
    performDelete(suggestionUrl(testData.czechSuggestions[0].self.id), null, bearerHeaders(token)).andIsForbidden
  }

  @Test
  fun `a token carrying translation-suggestions manage can delete another author's suggestion`() {
    val moderatorToken =
      tokens.issue(
        subject = testData.suggestionModerator.self.id,
        scopes = listOf("translations.view", "translation-suggestions.manage"),
        projectIds = null,
      )

    performDelete(suggestionUrl(testData.czechSuggestions[1].self.id), null, bearerHeaders(moderatorToken)).andIsOk
  }

  private fun suggestionUrl(suggestionId: Long) =
    "/v2/projects/${testData.project.id}/languages/${testData.czechLanguage.id}/" +
      "key/${testData.keys[0].self.id}/suggestion/$suggestionId"
}
