package io.tolgee.ee.api.v2.controllers

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.SsoTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SsoProviderControllerTest : AuthorizedControllerTest() {
  private lateinit var testData: SsoTestData

  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  @BeforeEach
  fun setup() {
    testData = SsoTestData()
    testDataService.saveTestData(testData.root)
    this.userAccount = testData.user
    loginAsUser(testData.user.username)
    enabledFeaturesProvider.forceEnabled = setOf(Feature.SSO)
  }

  @Test
  fun `creates and returns sso provider`() {
    performAuthPut(
      "/v2/organizations/${testData.organization.id}/sso",
      requestTenant(),
    ).andIsOk

    performAuthGet("/v2/organizations/${testData.organization.id}/sso")
      .andIsOk
      .andAssertThatJson {
        node("domainName").isEqualTo("google")
        node("clientId").isEqualTo("clientId")
        node("clientSecret").isEqualTo("clientSecret")
        node("authorizationUri").isEqualTo("authorization")
        node("tokenUri").isEqualTo("tokenUri")
        node("jwkSetUri").isEqualTo("jwkSetUri")
        node("isEnabled").isEqualTo(true)
      }
  }

  @Test
  fun `fails if user is not owner of organization`() {
    testDataService.saveTestData(testData.createUserNotOwner)
    this.userAccount = testData.userNotOwner
    loginAsUser(testData.userNotOwner.username)
    performAuthPut(
      "/v2/organizations/${testData.userNotOwnerOrganization.id}/sso",
      requestTenant(),
    ).andIsForbidden
  }

  fun requestTenant() =
    mapOf(
      "domainName" to "google",
      "clientId" to "clientId",
      "clientSecret" to "clientSecret",
      "authorizationUri" to "authorization",
      "redirectUri" to "redirectUri",
      "tokenUri" to "tokenUri",
      "jwkSetUri" to "jwkSetUri",
      "isEnabled" to true,
    )
}
