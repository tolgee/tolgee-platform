package io.tolgee.development.testDataBuilder.data

/**
 * A user with one project, for driving the OAuth2 consent screen in the browser.
 *
 * The OAuth client itself is not test data — it comes from `tolgee.oauth2.*` configuration.
 */
class OAuth2ConsentE2eData : BaseTestData(USERNAME, "OAuth2 Consent Project") {
  companion object {
    const val USERNAME = "oauth2ConsentUser"
  }
}
