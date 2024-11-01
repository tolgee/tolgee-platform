package io.tolgee.api.v2.controllers.v2KeyController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.KeyLanguageDisablingTestData
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.enums.TranslationState
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class KeyControllerLanguageDisablingTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: KeyLanguageDisablingTestData

  @BeforeEach
  fun setup() {
    testData = KeyLanguageDisablingTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.project.self }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `get disabled languages`() {
    performProjectAuthGet("keys/${testData.key.self.id}/disabled-languages")
      .andIsOk.andAssertThatJson {
        node("_embedded.languages[0].id").isEqualTo(testData.german.self.id)
        node("_embedded.languages[1].id").isEqualTo(testData.czech.self.id)
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `get all disabled languages`() {
    performProjectAuthGet("all-keys-with-disabled-languages")
      .andIsOk.andAssertThatJson {
        node("_embedded.keys") {
          isArray.hasSize(1)
          node("[0].disabledLanguages[0].id").isEqualTo(testData.german.self.id)
          node("[0].disabledLanguages[1].id").isEqualTo(testData.czech.self.id)
        }
      }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `set disabled languages`() {
    performProjectAuthPut(
      "keys/${testData.key.self.id}/disabled-languages",
      mapOf(
        "languageIds" to
          listOf(testData.french.self.id, testData.czech.self.id),
      ),
    ).andIsOk.andAssertThatJson {
      node("_embedded.languages[0].id").isEqualTo(testData.french.self.id)
      node("_embedded.languages[1].id").isEqualTo(testData.czech.self.id)
    }

    translationService.find(testData.key.self, testData.french.self).get()
      .state.assert.isEqualTo(TranslationState.DISABLED)
    translationService.find(testData.key.self, testData.french.self).get()
      .state.assert.isEqualTo(TranslationState.DISABLED)
    translationService.find(testData.key.self, testData.german.self).get()
      .state.assert.isEqualTo(TranslationState.UNTRANSLATED)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `cannot modify translation when disabled`() {
    performProjectAuthPut(
      "translations",
      SetTranslationsWithKeyDto(
        testData.key.self.name,
        null,
        translations = mapOf("de" to "bla"),
      ),
    ).andIsBadRequest.andHasErrorMessage(Message.CANNOT_MODIFY_DISABLED_TRANSLATION)
  }
}
