package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.LanguagePermissionsTestData
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class TranslationsControllerSuggestionsModeTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: LanguagePermissionsTestData

  fun initTestData(translationProtection: TranslationProtection = TranslationProtection.NONE) {
    testData = LanguagePermissionsTestData(projectTranslationProtection = translationProtection)
    this.projectSupplier = { testData.project }
    testDataService.saveTestData(testData.root)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `translator can update reviewed translation when suggestions mode is optional`() {
    initTestData(TranslationProtection.NONE)
    userAccount = testData.translateEnOnlyUser
    performUpdate("reviewedKey", "en").andIsOk
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `translator can't update reviewed translation when suggestions mode is enforced`() {
    initTestData(TranslationProtection.PROTECT_REVIEWED)
    userAccount = testData.translateEnOnlyUser
    performUpdate("reviewedKey", "en").andIsForbidden
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `translator can update unreviewed translation when suggestions mode is enforced`() {
    initTestData(TranslationProtection.PROTECT_REVIEWED)
    userAccount = testData.translateEnOnlyUser
    performUpdate("key", "en").andIsOk
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `translator can update empty translation when suggestions mode is enforced`() {
    initTestData(TranslationProtection.PROTECT_REVIEWED)
    userAccount = testData.translateEnOnlyUser
    performUpdate("key2", "en").andIsOk
  }

  private fun performUpdate(key: String, lang: String) =
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        key,
        null,
        mutableMapOf(lang to lang),
      ),
    )
}
