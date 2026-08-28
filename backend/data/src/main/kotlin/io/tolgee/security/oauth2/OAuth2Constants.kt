package io.tolgee.security.oauth2

object OAuth2Constants {
  /** Project-selection sentinel: not narrowed to any project subset (still bounded by live permissions). */
  const val ALL_PROJECTS = "*"

  /** Authorize-request parameter carrying the client's project hint. */
  const val PROJECT_PARAM = "project"

  const val BROWSER_EXTENSION_CLIENT_ID = "tolgee-browser-extension"
  const val CLI_CLIENT_ID = "tolgee-cli"

  /** SPA routes the authorization endpoint hands the browser to. */
  const val CONSENT_PAGE_PATH = "/oauth2/consent"
  const val BOOTSTRAP_PAGE_PATH = "/oauth2/bootstrap"
}
