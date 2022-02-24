package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.model.enums.ApiScope
import io.tolgee.model.enums.TranslationState
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class V2TranslationsControllerModificationTest : ProjectAuthControllerTest("/v2/projects/") {

  lateinit var testData: TranslationsTestData

  @BeforeEach
  fun setup() {
    testData = TranslationsTestData()
    this.projectSupplier = { testData.project }
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `sets translations for existing key`() {
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "A key", mutableMapOf("en" to "English")
      )
    ).andIsOk
      .andAssertThatJson {
        node("translations.en.text").isEqualTo("English")
        node("translations.en.id").isValidId
        node("keyId").isValidId
        node("keyName").isEqualTo("A key")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `returns selected languages after set`() {
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "A key", mutableMapOf("en" to "English"), setOf("en", "de")
      )
    ).andIsOk
      .andAssertThatJson {
        node("translations.en.text").isEqualTo("English")
        node("translations.de.text").isEqualTo("Z translation")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `validated translation length`() {
    val text = "a".repeat(10001)
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "A key", mutableMapOf("en" to text)
      )
    ).andIsBadRequest
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `sets translation state`() {
    val id = testData.aKeyGermanTranslation.id
    performProjectAuthPut("/translations/$id/set-state/UNTRANSLATED", null).andIsOk
      .andAssertThatJson {
        node("state").isEqualTo("UNTRANSLATED")
        node("id").isValidId.satisfies { id ->
          id.toLong().let {
            assertThat(translationService.find(it)?.state).isEqualTo(TranslationState.UNTRANSLATED)
          }
        }
      }
  }

  @ProjectApiKeyAuthTestMethod(scopes = [ApiScope.TRANSLATIONS_VIEW])
  @Test
  fun `sets translations for existing key API key forbidden`() {
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto("A key", mutableMapOf("en" to "English"))
    ).andIsForbidden
  }

  @ProjectApiKeyAuthTestMethod(scopes = [ApiScope.KEYS_EDIT, ApiScope.TRANSLATIONS_EDIT])
  @Test
  fun `sets translations for new key with API key`() {
    performProjectAuthPost(
      "/translations",
      SetTranslationsWithKeyDto("A key", mutableMapOf("en" to "English"))
    ).andIsOk
  }

  @ProjectApiKeyAuthTestMethod(scopes = [ApiScope.TRANSLATIONS_EDIT])
  @Test
  fun `sets translations for new key forbidden with api key`() {
    performProjectAuthPost(
      "/translations",
      SetTranslationsWithKeyDto("A key not existings", mutableMapOf("en" to "English"))
    ).andIsForbidden
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `sets untranslated state if null value provided`() {
    assertThat(translationService.find(testData.aKeyGermanTranslation.id)).isNotNull
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "A key", mutableMapOf("en" to "English", "de" to null)
      )
    ).andIsOk
      .andAssertThatJson {
        node("translations.en.text").isEqualTo("English")
        node("translations.en.id").isValidId
        node("translations.de.text").isEqualTo(null)
        node("translations.de.state").isEqualTo("UNTRANSLATED")
        node("keyId").isValidId
        node("keyName").isEqualTo("A key")
      }

    assertThat(translationService.find(testData.aKeyGermanTranslation.id)!!.state)
      .isEqualTo(TranslationState.UNTRANSLATED)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `sets translations for not existing key`() {
    performProjectAuthPost(
      "/translations",
      SetTranslationsWithKeyDto(
        "Super ultra cool new key", mutableMapOf("en" to "English")
      )
    ).andIsOk.andAssertThatJson {
      node("translations.en.text").isEqualTo("English")
      node("translations.en.id").isValidId
      node("keyId").isValidId
      node("keyName").isEqualTo("Super ultra cool new key")
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `sets translated state when non blank text is provided`() {
    testData.addUntranslated()
    testDataService.saveTestData(testData.root)
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "lala", mutableMapOf("en" to "English")
      )
    ).andIsOk.andAssertThatJson {
      node("translations.en.text").isEqualTo("English")
      node("translations.en.id").isValidId
      node("translations.en.state").isEqualTo("TRANSLATED")
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `removes the auto translated state`() {
    testDataService.saveTestData(testData.root)
    val translation = testData.aKeyGermanTranslation
    performProjectAuthPut(
      "/translations/${translation.id}/dismiss-auto-translated-state",
      null
    ).andIsOk
    val updatedTranslation = translationService.get(translation.id)
    assertThat(updatedTranslation.auto).isEqualTo(false)
    assertThat(updatedTranslation.mtProvider).isEqualTo(null)
  }
}
