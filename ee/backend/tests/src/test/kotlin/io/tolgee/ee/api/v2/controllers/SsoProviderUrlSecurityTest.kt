package io.tolgee.ee.api.v2.controllers

import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.SsoTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SsoProviderUrlSecurityTest : AuthorizedControllerTest() {
  private companion object {
    // UrlSecurity.validateUrl resolves the host via InetAddress.getAllByName; an IP literal is used
    // (not a hostname) so the positive cases never perform a real DNS lookup. TEST-NET-3 (203.0.113.0/24)
    // is reserved for documentation, is non-private, and never routes anywhere.
    const val PUBLIC_HOST = "203.0.113.10"
  }

  private lateinit var testData: SsoTestData

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    enabledFeaturesProvider.forceEnabled = setOf(Feature.SSO)
    tolgeeProperties.authentication.ssoOrganizations.enabled = true
    tolgeeProperties.authentication.ssoOrganizations.allowLocalAddresses = false
    internalProperties.disableUrlSsrfProtection = false
    testData = SsoTestData()
    testDataService.saveTestData(testData.root)
    this.userAccount = testData.userAdmin
  }

  @AfterEach
  fun tearDown() {
    testDataService.cleanTestData(testData.root)
    enabledFeaturesProvider.forceEnabled = null
    tolgeeProperties.authentication.ssoOrganizations.enabled = false
    tolgeeProperties.authentication.ssoOrganizations.allowLocalAddresses = false
    internalProperties.disableUrlSsrfProtection = true
  }

  @Test
  fun `rejects local provider urls when allowLocalAddresses is false`() {
    performAuthPut(
      ssoUrl(),
      requestTenant(authorizationUri = "http://localhost/authorize", tokenUri = "http://localhost/token"),
    ).andIsBadRequest.andHasErrorMessage(Message.URL_NOT_VALID)
  }

  @Test
  fun `rejects loopback ip token uri`() {
    performAuthPut(
      ssoUrl(),
      requestTenant(authorizationUri = "http://127.0.0.1/authorize", tokenUri = "http://127.0.0.1/token"),
    ).andIsBadRequest.andHasErrorMessage(Message.URL_NOT_VALID)
  }

  @Test
  fun `rejects non-http scheme token uri`() {
    performAuthPut(
      ssoUrl(),
      requestTenant(tokenUri = "ftp://localhost/token"),
    ).andIsBadRequest.andHasErrorMessage(Message.URL_NOT_VALID)
  }

  @Test
  fun `allows local provider urls when allowLocalAddresses is true`() {
    tolgeeProperties.authentication.ssoOrganizations.allowLocalAddresses = true
    performAuthPut(
      ssoUrl(),
      requestTenant(authorizationUri = "http://localhost/authorize", tokenUri = "http://localhost/token"),
    ).andIsOk.andAssertThatJson {
      node("tokenUri").isEqualTo("http://localhost/token")
    }
  }

  @Test
  fun `still rejects non-http scheme when allowLocalAddresses is true`() {
    tolgeeProperties.authentication.ssoOrganizations.allowLocalAddresses = true
    performAuthPut(
      ssoUrl(),
      requestTenant(tokenUri = "ftp://localhost/token"),
    ).andIsBadRequest.andHasErrorMessage(Message.URL_NOT_VALID)
  }

  @Test
  fun `validates authorization uri in addition to token uri`() {
    tolgeeProperties.authentication.ssoOrganizations.allowLocalAddresses = true
    performAuthPut(
      ssoUrl(),
      requestTenant(authorizationUri = "ftp://localhost/authorize", tokenUri = "http://localhost/token"),
    ).andIsBadRequest.andHasErrorMessage(Message.URL_NOT_VALID)
  }

  @Test
  fun `skips url validation when provider is disabled`() {
    performAuthPut(
      ssoUrl(),
      requestTenant(tokenUri = "http://localhost/token", enabled = false),
    ).andIsOk
  }

  @Test
  fun `accepts public provider urls under active ssrf protection`() {
    performAuthPut(
      ssoUrl(),
      requestTenant(authorizationUri = "https://$PUBLIC_HOST/authorize", tokenUri = "https://$PUBLIC_HOST/token"),
    ).andIsOk.andAssertThatJson {
      node("tokenUri").isEqualTo("https://$PUBLIC_HOST/token")
    }
  }

  @Test
  fun `rejects changing provider url to local on update`() {
    performAuthPut(
      ssoUrl(),
      requestTenant(authorizationUri = "https://$PUBLIC_HOST/authorize", tokenUri = "https://$PUBLIC_HOST/token"),
    ).andIsOk

    performAuthPut(
      ssoUrl(),
      requestTenant(authorizationUri = "https://$PUBLIC_HOST/authorize", tokenUri = "http://localhost/token"),
    ).andIsBadRequest.andHasErrorMessage(Message.URL_NOT_VALID)
  }

  private fun ssoUrl() = "/v2/organizations/${testData.organization.id}/sso"

  private fun requestTenant(
    authorizationUri: String = "http://localhost/authorize",
    tokenUri: String = "http://localhost/token",
    enabled: Boolean = true,
  ) = mapOf(
    "domain" to "google",
    "clientId" to "dummy_client_id",
    "clientSecret" to "clientSecret",
    "authorizationUri" to authorizationUri,
    "tokenUri" to tokenUri,
    "enabled" to enabled,
  )
}
