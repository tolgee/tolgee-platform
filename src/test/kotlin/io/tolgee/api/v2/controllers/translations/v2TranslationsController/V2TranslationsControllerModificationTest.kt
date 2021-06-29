package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.annotations.ProjectJWTAuthTestMethod
import io.tolgee.assertions.Assertions.assertThat
import io.tolgee.constants.ApiScope
import io.tolgee.controllers.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.dtos.request.SetTranslationsWithKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.model.enums.TranslationState
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

@SpringBootTest
@AutoConfigureMockMvc
class V2TranslationsControllerModificationTest : ProjectAuthControllerTest("/v2/projects/") {

    lateinit var testData: TranslationsTestData

    @BeforeMethod
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
                )).andIsOk
                .andAssertThatJson {
                    node("translations.en.text").isEqualTo("English")
                    node("translations.en.id").isValidId
                    node("keyId").isValidId
                    node("keyName").isEqualTo("A key")
                }
    }

    @ProjectJWTAuthTestMethod
    @Test
    fun `sets translation state`() {
        val id = testData.aKeyGermanTranslation.id
        performProjectAuthPut("/translations/${id}/set-state/UNTRANSLATED", null).andIsOk
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

    @ProjectApiKeyAuthTestMethod(scopes = [ApiScope.TRANSLATIONS_EDIT])
    @Test
    fun `sets translations for new key forbidden`() {
        performProjectAuthPost(
                "/translations",
                SetTranslationsWithKeyDto("A key", mutableMapOf("en" to "English"))
        ).andIsForbidden
    }

    @ProjectJWTAuthTestMethod
    @Test
    fun `removes translations if null value provided`() {
        assertThat(translationService.find(testData.aKeyGermanTranslation.id)).isNotNull
        performProjectAuthPut(
                "/translations",
                SetTranslationsWithKeyDto(
                        "A key", mutableMapOf("en" to "English", "de" to null)
                )).andIsOk
                .andAssertThatJson {
                    node("translations.en.text").isEqualTo("English")
                    node("translations.en.id").isValidId
                    node("keyId").isValidId
                    node("keyName").isEqualTo("A key")
                }

        assertThat(translationService.find(testData.aKeyGermanTranslation.id)).isNull()
    }

    @ProjectJWTAuthTestMethod
    @Test
    fun `sets translations for not existing key`() {
        performProjectAuthPost(
                "/translations",
                SetTranslationsWithKeyDto(
                        "Super ultra cool new key", mutableMapOf("en" to "English")
                )).andIsOk.andAssertThatJson {
            node("translations.en.text").isEqualTo("English")
            node("translations.en.id").isValidId
            node("keyId").isValidId
            node("keyName").isEqualTo("Super ultra cool new key")
        }
    }
}
