package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.dtos.request.pat.CreatePatDto
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsForbidden
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.satisfies
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.translation.Translation
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders

@SpringBootTest
@AutoConfigureMockMvc
class TranslationsControllerModificationTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TranslationsTestData

  @BeforeEach
  fun setup() {
    testData = TranslationsTestData()
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `sets translations for existing key`() {
    saveTestData()
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "A key",
        null,
        mutableMapOf("en" to "English"),
      ),
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
  fun `validates plurals on set for existing`() {
    testData.addPluralKey()
    saveTestData()
    performUpdatePluralKey("Not a plural").andIsBadRequest
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `normalizes plurals on set for existing`() {
    testData.addPluralKey()
    saveTestData()
    performUpdatePluralKey("Hello! {count, plural, other {test}}")
      .andIsOk
      .andAssertThatJson {
        node("translations.en.text").isString.isEqualTo("{count, plural,\nother {Hello! test}\n}")
        node("keyIsPlural").isBoolean.isTrue
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `doesnt touch isPlural if not plural`() {
    saveTestData()
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "A key",
        null,
        mutableMapOf("en" to "English"),
      ),
    ).andIsOk
      .andAssertThatJson {
        node("translations.en.text").isEqualTo("English")
        node("keyIsPlural").isBoolean.isFalse
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `works with no other`() {
    testData.addPluralKey()
    saveTestData()
    performUpdatePluralKey(
      "{count, plural,\n" +
        "one {test}\n" +
        "other{}}",
    ).andIsOk
      .andAssertThatJson {
        node("translations.en.text").isString.isEqualTo("{count, plural,\none {test}\nother {}\n}")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `works with empty string`() {
    val key = testData.addPluralKey()
    saveTestData()
    performUpdatePluralKey("")
      .andIsOk
      .andAssertThatJson {
        node("translations.en.text").isEqualTo(null)
      }

    executeInNewTransaction {
      keyService
        .get(key.id)
        .translations
        .find { it.language.tag == "en" }!!
        .text.assert
        .isNull()
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `creates key with correct isPlural for new keys`() {
    saveTestData()
    performProjectAuthPost(
      "/translations",
      SetTranslationsWithKeyDto(
        "plural_key",
        null,
        mutableMapOf(
          "en" to "Hi! {count, plural, other {test}}",
          "de" to "Nicht ein Plural",
        ),
      ),
    ).andIsOk.andPrettyPrint.andAssertThatJson {
      node("translations.en.text").isString.isEqualTo("{count, plural,\nother {Hi! test}\n}")
      node("translations.de.text").isString.isEqualTo("{count, plural,\nother {Nicht ein Plural}\n}")
      node("keyIsPlural").isBoolean.isTrue
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `creates key with correct isPlural for new keys (not plural)`() {
    saveTestData()
    performProjectAuthPost(
      "/translations",
      SetTranslationsWithKeyDto(
        "other key",
        null,
        mutableMapOf(
          "de" to "Nicht ein Plural",
        ),
      ),
    ).andIsOk.andPrettyPrint.andAssertThatJson {
      node("translations.de.text").isString.isEqualTo("Nicht ein Plural")
      node("keyIsPlural").isBoolean.isFalse
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `returns selected languages after set`() {
    saveTestData()
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "A key",
        null,
        mutableMapOf("en" to "English"),
        setOf("en", "de"),
      ),
    ).andIsOk
      .andAssertThatJson {
        node("translations.en.text").isEqualTo("English")
        node("translations.de.text").isEqualTo("Z translation")
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `validated translation length`() {
    saveTestData()
    val text = "a".repeat(10001)
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "A key",
        null,
        mutableMapOf("en" to text),
      ),
    ).andIsBadRequest
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `sets translation state`() {
    saveTestData()
    val id = testData.aKeyGermanTranslation.id
    performProjectAuthPut("/translations/$id/set-state/TRANSLATED", null)
      .andIsOk
      .andAssertThatJson {
        node("state").isEqualTo("TRANSLATED")
        node("id").isValidId.satisfies { id ->
          id.toLong().let {
            assertThat(translationService.find(it)?.state).isEqualTo(TranslationState.TRANSLATED)
          }
        }
      }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `cannot set UNTRANSLATED when contains value`() {
    saveTestData()
    val id = testData.aKeyGermanTranslation.id
    performProjectAuthPut("/translations/$id/set-state/UNTRANSLATED", null)
      .andIsBadRequest
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_VIEW])
  @Test
  fun `sets translations for existing key API key forbidden`() {
    saveTestData()
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto("A key", null, mutableMapOf("en" to "English")),
    ).andIsForbidden
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.KEYS_EDIT, Scope.TRANSLATIONS_EDIT])
  @Test
  fun `sets translations for new key with API key`() {
    saveTestData()
    performProjectAuthPost(
      "/translations",
      SetTranslationsWithKeyDto("A key", null, mutableMapOf("en" to "English")),
    ).andIsOk
  }

  @Test
  fun `sets translations for new key with PAT`() {
    testDataService.saveTestData(testData.root)
    val pat = patService.create(CreatePatDto("hello"), testData.user)
    performPut(
      "/v2/projects/${testData.project.id}/translations",
      SetTranslationsWithKeyDto("A key", null, mutableMapOf("en" to "English")),
      HttpHeaders().apply {
        add("X-API-Key", "tgpat_${pat.token}")
      },
    ).andIsOk
  }

  @ProjectApiKeyAuthTestMethod(scopes = [Scope.TRANSLATIONS_EDIT])
  @Test
  fun `sets translations for new key forbidden with api key`() {
    saveTestData()
    performProjectAuthPost(
      "/translations",
      SetTranslationsWithKeyDto("A key not existings", null, mutableMapOf("en" to "English")),
    ).andIsForbidden
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `sets untranslated state if null value provided`() {
    saveTestData()
    assertThat(translationService.find(testData.aKeyGermanTranslation.id)).isNotNull
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "A key",
        null,
        mutableMapOf("en" to "English", "de" to null),
      ),
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
    saveTestData()
    performProjectAuthPost(
      "/translations",
      SetTranslationsWithKeyDto(
        "Super ultra cool new key",
        null,
        mutableMapOf("en" to "English"),
      ),
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
    saveTestData()
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "lala",
        null,
        mutableMapOf("en" to "English"),
      ),
    ).andIsOk.andAssertThatJson {
      node("translations.en.text").isEqualTo("English")
      node("translations.en.id").isValidId
      node("translations.en.state").isEqualTo("TRANSLATED")
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `updates outdated flag and base state when base updated`() {
    testData.addTranslationsWithStates()
    saveTestData()
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        key = "state test key",
        namespace = null,
        translations = mutableMapOf("en" to "b"),
        languagesToReturn = setOf("en", "de"),
      ),
    ).andAssertThatJson {
      node("translations.en.outdated").isEqualTo(false)
      node("translations.en.state").isEqualTo("TRANSLATED")
      node("translations.de.outdated").isEqualTo(true)
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `doesn't update outdated flag when base updated`() {
    testData.addTranslationsWithStates()
    saveTestData()
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        key = "state test key",
        namespace = null,
        translations = mutableMapOf("de" to "new"),
        languagesToReturn = setOf("en", "de"),
      ),
    ).andAssertThatJson {
      node("translations.en.outdated").isEqualTo(false)
      node("translations.de.outdated").isEqualTo(false)
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `removes the auto translated state`() {
    saveTestData()
    val translation = testData.aKeyGermanTranslation
    performProjectAuthPut(
      "/translations/${translation.id}/dismiss-auto-translated-state",
      null,
    ).andIsOk
    val updatedTranslation = translationService.get(translation.id)
    assertThat(updatedTranslation.auto).isEqualTo(false)
    assertThat(updatedTranslation.mtProvider).isEqualTo(null)
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `sets outdated flag`() {
    saveTestData()
    val translation = testData.aKeyGermanTranslation
    testOutdated(translation, false)
    testOutdated(translation, true)
  }

  private fun testOutdated(
    translation: Translation,
    state: Boolean,
  ) {
    performProjectAuthPut(
      "/translations/${translation.id}/set-outdated-flag/$state",
      null,
    ).andIsOk
    val updatedTranslation = translationService.get(translation.id)
    assertThat(updatedTranslation.outdated).isEqualTo(state)
  }

  private fun saveTestData() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.project }
  }

  private fun performUpdatePluralKey(value: String?) =
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "plural_key",
        null,
        mutableMapOf("en" to value),
      ),
    )
}
