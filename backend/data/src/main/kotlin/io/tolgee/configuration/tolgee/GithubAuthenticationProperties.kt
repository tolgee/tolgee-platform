package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty

@DocProperty(
  prefix = "tolgee.authentication.github",
  description =
    ":::info\n" +
      "GitHub authentication can be used in combination with native authentication.\n" +
      ":::\n\n",
  displayName = "GitHub",
)
class GithubAuthenticationProperties {
  @DocProperty(description = "OAuth Client ID, obtained in GitHub administration.")
  var clientId: String? = null

  @DocProperty(description = "OAuth Client secret, obtained in GitHub administration.")
  var clientSecret: String? = null

  @DocProperty(
    description =
      "URL to the OAuth authorization screen. " +
        "Useful if you want to authenticate against a self-hosted GitHub Enterprise Server.",
  )
  var authorizationUrl: String = "https://github.com/login/oauth/access_token"

  @DocProperty(
    description =
      "URL to GitHub's `/user` API endpoint. " +
        "Useful if you want to authenticate against a self-hosted GitHub Enterprise Server.",
  )
  var userUrl: String = "https://api.github.com/user"
}
