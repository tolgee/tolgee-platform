package io.tolgee.development.testDataBuilder.data

/**
 * A user with one project, for driving the OAuth2 consent screen in the browser.
 *
 * The OAuth client itself is not test data: clients come from configuration, so the e2e deployment registers the
 * browser-extension client via `tolgee.oauth2.browser-extension-redirect-uris` (see e2e/docker-compose.yml).
 */
class OAuth2ConsentE2eData : BaseTestData("oauth2ConsentUser", "OAuth2 Consent Project") {
  init {
    root.apply {
      projectBuilder.apply {
        addKey {
          name = "oauth2-consent-key"
        }.build {
          addTranslation {
            language = projectBuilder.self.baseLanguage!!
            text = "Hello"
          }
        }
      }
    }
  }
}
