package io.tolgee.controllers

import io.tolgee.development.testDataBuilder.data.AuthProviderChangeTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc

@AutoConfigureMockMvc
class AuthProviderChangeControllerTest : AuthorizedControllerTest() {
  private lateinit var testData: AuthProviderChangeTestData

  @BeforeEach
  fun setup() {
    setForcedDate()
    testData = AuthProviderChangeTestData(currentDateProvider.date)
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @AfterEach
  fun tearDown() {
    testDataService.cleanTestData(testData.root)
    clearForcedDate()
  }

  @Test
  fun `gets current authentication provider`() {
    userAccount = testData.userGithub
    performAuthGet("/v2/auth-provider").andIsOk.andAssertThatJson {
      node("id").isNull()
      node("authType").isString.isEqualTo("GITHUB")
    }
  }

  @Test
  fun `doesn't get when there is no provider`() {
    userAccount = testData.userNoProvider
    performAuthGet("/v2/auth-provider").andIsNotFound
  }

  @Test
  fun `gets pending authentication provider change`() {
    userAccount = testData.userChangeNoneToGithub
    performAuthGet("/v2/auth-provider/change").andIsOk.andAssertThatJson {
      node("id").isString.isEqualTo(testData.changeNoneToGithub.identifier)
      node("authType").isString.isEqualTo("GITHUB")
    }
  }

  @Test
  fun `doesn't get when there is no pending provider change`() {
    userAccount = testData.userNoProvider
    performAuthGet("/v2/auth-provider/change").andIsNotFound
  }

  @Test
  fun `doesn't get when pending change is expired`() {
    userAccount = testData.userChangeExpired
    performAuthGet("/v2/auth-provider/change").andIsNotFound
  }

  @Test
  fun `deletes pending change`() {
    userAccount = testData.userChangeNoneToGithub
    performAuthDelete("/v2/auth-provider/change").andIsOk
    performAuthGet("/v2/auth-provider/change").andIsNotFound
  }

  @Test
  fun `doesn't delete when there is no pending change`() {
    userAccount = testData.userNoProvider
    performAuthDelete("/v2/auth-provider/change").andIsNotFound
    performAuthGet("/v2/auth-provider/change").andIsNotFound
  }

  @Test
  fun `doesn't delete expired pending change`() {
    userAccount = testData.userChangeExpired
    performAuthDelete("/v2/auth-provider/change").andIsNotFound
    performAuthGet("/v2/auth-provider/change").andIsNotFound
  }

  @Test
  fun `doesn't accept expired pending change`() {
    userAccount = testData.userChangeExpired
    performAuthPost("/v2/auth-provider/change", mapOf("id" to testData.changeExpired.identifier)).andIsNotFound
  }

  @Test
  fun `accepts change none to github`() {
    userAccount = testData.userChangeNoneToGithub
    performAuthGet("/v2/auth-provider").andIsNotFound
    performAuthPost("/v2/auth-provider/change", mapOf("id" to testData.changeNoneToGithub.identifier)).andIsOk
    performAuthGet("/v2/auth-provider/change").andIsNotFound
    performAuthGet("/v2/auth-provider").andIsOk.andAssertThatJson {
      node("authType").isString.isEqualTo("GITHUB")
    }
  }

  @Test
  fun `accepts change github to none`() {
    userAccount = testData.userChangeGithubToNone
    performAuthGet("/v2/auth-provider").andIsOk.andAssertThatJson {
      node("authType").isString.isEqualTo("GITHUB")
    }
    performAuthPost("/v2/auth-provider/change", mapOf("id" to testData.changeGithubToNone.identifier)).andIsOk
    performAuthGet("/v2/auth-provider/change").andIsNotFound
    performAuthGet("/v2/auth-provider").andIsNotFound
  }

  @Test
  fun `doesn't accept change from github to none when password is not set`() {
    userAccount = testData.userChangeGithubToNoneNoPassword
    performAuthGet("/v2/auth-provider").andIsOk.andAssertThatJson {
      node("authType").isString.isEqualTo("GITHUB")
    }
    performAuthPost(
      "/v2/auth-provider/change",
      mapOf("id" to testData.changeGithubToNoneNoPassword.identifier),
    ).andIsBadRequest
    performAuthGet("/v2/auth-provider/change").andIsOk
    performAuthGet("/v2/auth-provider").andIsOk.andAssertThatJson {
      node("authType").isString.isEqualTo("GITHUB")
    }
  }
}
