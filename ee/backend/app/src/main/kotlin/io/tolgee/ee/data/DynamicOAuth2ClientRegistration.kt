package io.tolgee.ee.data

import org.springframework.security.oauth2.client.registration.ClientRegistration

class DynamicOAuth2ClientRegistration(
  var tenantId: String,
  var clientRegistration: ClientRegistration,
)
