package io.tolgee.ee.data

import io.tolgee.ee.model.Tenant
import org.springframework.security.oauth2.client.registration.ClientRegistration

class DynamicOAuth2ClientRegistration(
  var tenant: Tenant,
  var clientRegistration: ClientRegistration,
)
