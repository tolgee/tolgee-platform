package io.tolgee.configuration.tolgee

import io.tolgee.configuration.annotations.DocProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "tolgee.authentication.google")
@DocProperty(
  description =
    "The following instructions explain how to set up Google OAuth. " +
      "[Setting up OAuth 2.0](https://support.google.com/cloud/answer/6158849).",
  displayName = "Google",
)
class GoogleAuthenticationProperties {
  @DocProperty(description = "OAuth Client ID, obtained in Google Cloud Console.")
  var clientId: String? = null

  @DocProperty(description = "OAuth Client secret, obtained in Google Cloud Console.")
  var clientSecret: String? = null

  @DocProperty(
    description =
      "The registration can be limited to users of a Google Workspace domain. " +
        "Multiple Google Workspace domains can be separated by a comma `,`. " +
        "If nothing is set, anyone can log in with their Google account.",
  )
  var workspaceDomain: String? = null

  @DocProperty(description = "URL to Google `/token` API endpoint. This usually does not need to be changed.")
  var authorizationUrl: String = "https://oauth2.googleapis.com/token"

  @DocProperty(description = "URL to Google `/userinfo` API endpoint. This usually does not need to be changed.")
  var userUrl: String = "https://www.googleapis.com/oauth2/v3/userinfo"
}
