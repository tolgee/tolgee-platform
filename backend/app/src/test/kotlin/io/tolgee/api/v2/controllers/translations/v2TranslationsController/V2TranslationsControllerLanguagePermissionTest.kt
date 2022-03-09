package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.LanguagePermissionsTestData
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class V2TranslationsControllerLanguagePermissionTest : ProjectAuthControllerTest("/v2/projects/") {

  lateinit var testData: LanguagePermissionsTestData

  @BeforeEach
  fun setup() {
    testData = LanguagePermissionsTestData()
    this.projectSupplier = { testData.project }
    testDataService.saveTestData(testData.root)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `denies access for user without the language permission - update`() {
    userAccount = testData.enOnlyUser
    performUpdate("de").andIsForbidden
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `allows access for user with language permission - update`() {
    userAccount = testData.enOnlyUser
    performUpdate("en").andIsOk
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `allows access for user with all language permissions - update`() {
    userAccount = testData.allLangUser
    performUpdate("en").andIsOk
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `denies access for user without the language permission - set state`() {
    userAccount = testData.enOnlyUser
    performSetState(testData.germanTranslation.id).andIsForbidden
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `allows access for user with language permission - set state`() {
    userAccount = testData.enOnlyUser
    performSetState(testData.englishTranslation.id).andIsOk
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `allows access for user with all language permissions - set state`() {
    userAccount = testData.allLangUser
    performSetState(testData.englishTranslation.id).andIsOk
  }

  private fun performUpdate(lang: String) = performProjectAuthPut(
    "/translations",
    SetTranslationsWithKeyDto(
      "key", mutableMapOf(lang to lang)
    )
  )

  private fun performSetState(translationId: Long) = performProjectAuthPut(
    "/translations/$translationId/set-state/REVIEWED"
  )
}
