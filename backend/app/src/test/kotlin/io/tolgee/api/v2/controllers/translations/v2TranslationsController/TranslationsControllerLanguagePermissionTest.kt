package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.LanguagePermissionsTestData
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class TranslationsControllerLanguagePermissionTest : ProjectAuthControllerTest("/v2/projects/") {
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
    userAccount = testData.translateEnOnlyUser
    performUpdate("de").andIsForbidden
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `allows access for user with language permission - update`() {
    userAccount = testData.translateEnOnlyUser
    performUpdate("en").andIsOk
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `allows access for user with all language permissions - update`() {
    userAccount = testData.translateAllUser
    performUpdate("en").andIsOk
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `denies access for user without the language permission - set state`() {
    userAccount = testData.translateEnOnlyUser
    performSetState(testData.germanTranslation.id).andIsForbidden
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `allows access for user with language permission - set state`() {
    userAccount = testData.reviewEnOnlyUser
    performSetState(testData.englishTranslation.id).andIsOk
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `denies access for user with language permission - set state`() {
    userAccount = testData.translateEnOnlyUser
    performSetState(testData.englishTranslation.id).andIsForbidden
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `denies access for user with all language permissions - set state`() {
    userAccount = testData.translateAllUser
    performSetState(testData.englishTranslation.id).andIsForbidden
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `allows access for user with all language permissions - set state`() {
    userAccount = testData.reviewAllUser
    performSetState(testData.englishTranslation.id).andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns only permitted languages (all translation endpoint)`() {
    userAccount = testData.viewEnOnlyUser
    performProjectAuthGet("/translations/en,de")
      .andAssertThatJson {
        node("de").isAbsent()
        node("en").isPresent
      }.andIsOk
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns only permitted languages (translation view endpoint)`() {
    userAccount = testData.viewEnOnlyUser
    performProjectAuthGet("/translations?languages=en&languages=de")
      .andAssertThatJson {
        node("_embedded.keys[0].translations.de").isAbsent()
        node("_embedded.keys[0].translations.en").isPresent
      }.andIsOk
  }

  private fun performUpdate(lang: String) =
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "key",
        null,
        mutableMapOf(lang to lang),
      ),
    )

  private fun performSetState(translationId: Long) =
    performProjectAuthPut(
      "/translations/$translationId/set-state/REVIEWED",
    )
}
