package io.tolgee.ee.api.v2.controllers

import io.tolgee.constants.Feature
import io.tolgee.development.testDataBuilder.data.AuthProviderChangeEeTestData
import io.tolgee.ee.component.PublicEnabledFeaturesProvider
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsNoContent
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AuthProviderChangeControllerEeTest : AuthorizedControllerTest() {
  @Autowired
  private lateinit var enabledFeaturesProvider: PublicEnabledFeaturesProvider

  private lateinit var testData: AuthProviderChangeEeTestData

  @BeforeEach
  fun setup() {
    setForcedDate()

    enabledFeaturesProvider.forceEnabled = setOf(Feature.SSO)
    tolgeeProperties.authentication.ssoOrganizations.enabled = true
    tolgeeProperties.authentication.ssoGlobal.enabled = true

    testData = AuthProviderChangeEeTestData(currentDateProvider.date)
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @AfterEach
  fun tearDown() {
    enabledFeaturesProvider.forceEnabled = null
    tolgeeProperties.authentication.ssoOrganizations.enabled = false
    tolgeeProperties.authentication.ssoGlobal.enabled = false

    testDataService.cleanTestData(testData.root)
    clearForcedDate()
  }

  @Test
  fun `gets current authentication provider with sso domain for organizations sso`() {
    userAccount = testData.userSsoOrganizations
    performAuthGet("/v2/auth-provider").andIsOk.andAssertThatJson {
      node("id").isNull()
      node("authType").isString.isEqualTo("SSO")
      node("ssoDomain").isString.isEqualTo("domain-org.com")
    }
  }

  @Test
  fun `gets current authentication provider with sso domain for global sso`() {
    userAccount = testData.userSsoGlobal
    performAuthGet("/v2/auth-provider").andIsOk.andAssertThatJson {
      node("id").isNull()
      node("authType").isString.isEqualTo("SSO_GLOBAL")
      node("ssoDomain").isString.isEqualTo("domain.com")
    }
  }

  @Test
  fun `accepts change none to global sso`() {
    userAccount = testData.userChangeNoneToSsoGlobal
    performAuthGet("/v2/user/managed-by").andIsNoContent
    performAuthGet("/v2/auth-provider").andIsNotFound
    performAuthPost("/v2/auth-provider/change", mapOf("id" to testData.changeNoneToSsoGlobal.identifier)).andIsOk
    performAuthGet("/v2/auth-provider/change").andIsNotFound
    performAuthGet("/v2/auth-provider").andIsOk.andAssertThatJson {
      node("authType").isString.isEqualTo("SSO_GLOBAL")
      node("ssoDomain").isString.isEqualTo("domain.com")
    }
    performAuthGet("/v2/user/managed-by").andIsNoContent
  }

  @Test
  fun `accepts change none to organizations sso`() {
    userAccount = testData.userChangeNoneToSsoOrganizations
    performAuthGet("/v2/user/managed-by").andIsNoContent
    performAuthGet("/v2/auth-provider").andIsNotFound
    performAuthPost("/v2/auth-provider/change", mapOf("id" to testData.changeNoneToSsoOrganizations.identifier)).andIsOk
    performAuthGet("/v2/auth-provider/change").andIsNotFound
    performAuthGet("/v2/auth-provider").andIsOk.andAssertThatJson {
      node("authType").isString.isEqualTo("SSO")
      node("ssoDomain").isString.isEqualTo("domain-org.com")
    }
    performAuthGet("/v2/user/managed-by").andIsOk.andAssertThatJson {
      node("name").isString.isEqualTo(testData.organization.name)
    }
  }

  @Test
  fun `accepts change google to global sso`() {
    userAccount = testData.userChangeGoogleToSsoGlobal
    performAuthGet("/v2/user/managed-by").andIsNoContent
    performAuthGet("/v2/auth-provider").andIsOk.andAssertThatJson {
      node("authType").isString.isEqualTo("GOOGLE")
    }
    performAuthPost("/v2/auth-provider/change", mapOf("id" to testData.changeGoogleToSsoGlobal.identifier)).andIsOk
    performAuthGet("/v2/auth-provider/change").andIsNotFound
    performAuthGet("/v2/auth-provider").andIsOk.andAssertThatJson {
      node("authType").isString.isEqualTo("SSO_GLOBAL")
      node("ssoDomain").isString.isEqualTo("domain.com")
    }
    performAuthGet("/v2/user/managed-by").andIsNoContent
  }

  @Test
  fun `accepts change google to organizations sso`() {
    userAccount = testData.userChangeOauth2ToSsoOrganizations
    performAuthGet("/v2/user/managed-by").andIsNoContent
    performAuthGet("/v2/auth-provider").andIsOk.andAssertThatJson {
      node("authType").isString.isEqualTo("OAUTH2")
    }
    performAuthPost(
      "/v2/auth-provider/change",
      mapOf("id" to testData.changeOauth2ToSsoOrganizations.identifier),
    ).andIsOk
    performAuthGet("/v2/auth-provider/change").andIsNotFound
    performAuthGet("/v2/auth-provider").andIsOk.andAssertThatJson {
      node("authType").isString.isEqualTo("SSO")
      node("ssoDomain").isString.isEqualTo("domain-org.com")
    }
    performAuthGet("/v2/user/managed-by").andIsOk.andAssertThatJson {
      node("name").isString.isEqualTo(testData.organization.name)
    }
  }
}
