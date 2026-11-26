package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Pat
import io.tolgee.model.enums.ThirdPartyAuthType
import java.util.Date

class McpForcedSsoTestData(
  userName: String,
  forcedDomain: String? = null,
  thirdPartyAuth: ThirdPartyAuthType? = null,
) : BaseTestData(userName = userName) {
  lateinit var pat: Pat

  init {
    userAccountBuilder.build {
      self.thirdPartyAuthType = thirdPartyAuth
      if (thirdPartyAuth != null) {
        // An SSO account without a live SSO session fails token auth entirely.
        self.ssoSessionExpiry = Date(System.currentTimeMillis() + 10 * 60 * 1000)
      }
      addPat {
        description = "MCP test PAT"
        pat = this
      }
    }
    if (forcedDomain != null) {
      userAccountBuilder.defaultOrganizationBuilder.setTenant {
        domain = forcedDomain
        clientId = "dummy_client_id"
        clientSecret = "clientSecret"
        authorizationUri = "https://dummy-url.com"
        tokenUri = "http://tokenUri"
        force = true
      }
    }
  }
}
