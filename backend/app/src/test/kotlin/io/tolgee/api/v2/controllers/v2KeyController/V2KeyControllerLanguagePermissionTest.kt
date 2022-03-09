package io.tolgee.api.v2.controllers.v2KeyController

import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.LanguagePermissionsTestData
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.ResultActions

@SpringBootTest
@AutoConfigureMockMvc
class V2KeyControllerLanguagePermissionTest : ProjectAuthControllerTest("/v2/projects/") {

  lateinit var testData: LanguagePermissionsTestData

  @BeforeEach
  fun setup() {
    testData = LanguagePermissionsTestData()
    testDataService.saveTestData(testData.root)
    this.projectSupplier = { testData.project }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `denies access for user without the language permission - Token Auth`() {
    userAccount = testData.enOnlyUser
    performUpdate("de").andIsForbidden
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `allows access for user with language permission - Token Auth`() {
    userAccount = testData.enOnlyUser
    performUpdate("en").andIsOk
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `allows access for user with all language permissions - Token Auth`() {
    userAccount = testData.allLangUser
    performUpdate("en").andIsOk
  }

  @ProjectApiKeyAuthTestMethod
  @Test
  fun `denies access for user without the language permission - API key`() {
    userAccount = testData.enOnlyUser
    performUpdate("de").andIsForbidden
  }

  @ProjectApiKeyAuthTestMethod
  @Test
  fun `allows access for user with language permission - API key`() {
    userAccount = testData.enOnlyUser
    performUpdate("en").andIsOk
  }

  @ProjectApiKeyAuthTestMethod
  @Test
  fun `allows access for user with all language permissions - API key`() {
    userAccount = testData.allLangUser
    performUpdate("en").andIsOk
  }

  fun performUpdate(langTag: String): ResultActions {
    val key = testData.englishTranslation.key
    val keyId = key.id
    return performProjectAuthPut(
      "keys/$keyId/complex-update",
      mapOf(
        "name" to key.name,
        "translations" to mapOf(langTag to langTag)
      )
    )
  }
}
