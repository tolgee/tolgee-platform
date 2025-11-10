package io.tolgee.api.v2.controllers.v2KeyController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.constants.Message
import io.tolgee.development.testDataBuilder.data.KeysTestData
import io.tolgee.dtos.request.key.ComplexEditKeyDto
import io.tolgee.dtos.request.key.CreateKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andHasErrorMessage
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.isValidId
import io.tolgee.model.enums.Scope
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import io.tolgee.util.generateImage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.InputStreamSource

private const val RAW_PLURAL = "I have {dogsCount, plural, one {# dog} other {# dogs}}."
private const val NORMALIZED_PLURAL =
  "{dogsCount, plural,\n" +
    "one {I have # dog.}\n" +
    "other {I have # dogs.}\n" +
    "}"

@SpringBootTest
@AutoConfigureMockMvc
class KeyControllerPluralizationTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: KeysTestData

  val screenshotFile: InputStreamSource by lazy {
    generateImage(2000, 3000)
  }

  @BeforeEach
  fun setup() {
    testData = KeysTestData()
  }

  private fun saveAndPrepare() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.project }
  }

  @ProjectApiKeyAuthTestMethod(
    scopes = [
      Scope.KEYS_EDIT,
      Scope.TRANSLATIONS_EDIT,
    ],
  )
  @Test
  fun `validates incoming plurals`() {
    saveAndPrepare()
    val keyName = "super_key"

    performProjectAuthPut(
      "keys/${testData.keyWithReferences.id}/complex-update",
      ComplexEditKeyDto(
        name = keyName,
        translations = mapOf("en" to "Not a plural"),
        isPlural = true,
      ),
    ).andIsBadRequest.andAssertThatJson {
      node("code").isEqualTo("invalid_plural_form")
      node("params").isEqualTo(" [ [ \"Not a plural\" ] ]")
    }
  }

  @ProjectApiKeyAuthTestMethod(
    scopes = [
      Scope.KEYS_EDIT,
      Scope.TRANSLATIONS_EDIT,
    ],
  )
  @Test
  fun `normalizes incoming plurals`() {
    saveAndPrepare()
    val keyName = "super_key"

    performProjectAuthPut(
      "keys/${testData.keyWithReferences.id}/complex-update",
      ComplexEditKeyDto(
        name = keyName,
        translations = mapOf("en" to RAW_PLURAL),
        isPlural = true,
      ),
    ).andIsOk.andAssertThatJson {
      node("isPlural").isBoolean.isTrue
      node("translations.en.text").isString.isEqualTo(NORMALIZED_PLURAL)
    }
  }

  @ProjectApiKeyAuthTestMethod(
    scopes = [
      Scope.KEYS_EDIT,
      Scope.TRANSLATIONS_EDIT,
    ],
  )
  @Test
  fun `converts existing translations`() {
    val key =
      testData.projectBuilder
        .addKey {
          name = "plural_test_key"
        }.build {
          addTranslation("en", "Oh")
          addTranslation("de", RAW_PLURAL)
        }
    testData.projectBuilder.addCzech()

    saveAndPrepare()

    val keyName = "plural_test_key"
    performProjectAuthPut(
      "keys/${key.self.id}/complex-update",
      ComplexEditKeyDto(
        name = keyName,
        translations = mapOf("cs" to RAW_PLURAL),
        isPlural = true,
      ),
    ).andIsOk.andAssertThatJson {
      node("isPlural").isBoolean.isTrue
      node("translations.en.text").isString.isEqualTo("{value, plural,\nother {Oh}\n}")
      node("translations.de.text").isString.isEqualTo(NORMALIZED_PLURAL)
      node("translations.cs.text").isString.isEqualTo(NORMALIZED_PLURAL)
    }

    keyService
      .get(key.self.id)
      .isPlural.assert
      .isEqualTo(true)
  }

  @ProjectApiKeyAuthTestMethod(
    scopes = [
      Scope.KEYS_EDIT,
      Scope.TRANSLATIONS_EDIT,
    ],
  )
  @Test
  fun `warns on data loss when disabling plural`() {
    val key =
      testData.projectBuilder
        .addKey {
          name = "plural_test_key"
          isPlural = true
          pluralArgName = "dogsCount"
        }.build {
          addTranslation("de", RAW_PLURAL)
        }
    testData.projectBuilder.addCzech()

    saveAndPrepare()

    val keyName = "plural_test_key"
    performProjectAuthPut(
      "keys/${key.self.id}/complex-update",
      ComplexEditKeyDto(
        name = keyName,
        isPlural = false,
        warnOnDataLoss = true,
      ),
    ).andIsBadRequest.andHasErrorMessage(message = Message.PLURAL_FORMS_DATA_LOSS)
  }

  @ProjectApiKeyAuthTestMethod(
    scopes = [
      Scope.KEYS_EDIT,
      Scope.TRANSLATIONS_EDIT,
    ],
  )
  @Test
  fun `change to argName works`() {
    val key =
      testData.projectBuilder
        .addKey {
          name = "plural_test_key"
          isPlural = true
          pluralArgName = "dogsCount"
        }.build {
          addTranslation("en", RAW_PLURAL)
          addTranslation("de", RAW_PLURAL)
        }
    testData.projectBuilder.addCzech()

    saveAndPrepare()

    val keyName = "plural_test_key"
    performProjectAuthPut(
      "keys/${key.self.id}/complex-update",
      ComplexEditKeyDto(
        name = keyName,
        isPlural = true,
        pluralArgName = "catsCount",
      ),
    ).andIsOk.andAssertThatJson {
      node("isPlural").isBoolean.isTrue
      node("translations.de.text")
        .isString
        .isEqualTo(NORMALIZED_PLURAL.replace("dogsCount", "catsCount"))
    }

    keyService
      .get(key.self.id)
      .pluralArgName.assert
      .isEqualTo("catsCount")
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `normalizes on create`() {
    saveAndPrepare()
    performProjectAuthPost(
      "keys",
      CreateKeyDto(
        name = "new_key",
        translations = mapOf("en" to RAW_PLURAL),
        isPlural = true,
      ),
    ).andIsCreated.andAssertThatJson {
      node("id").isValidId
      node("name").isEqualTo("new_key")
      node("isPlural").isBoolean.isTrue
      node("translations.en.text").isString.isEqualTo(NORMALIZED_PLURAL)
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `validates on create`() {
    saveAndPrepare()
    performProjectAuthPost(
      "keys",
      CreateKeyDto(
        name = "new_key",
        translations =
          mapOf(
            "en" to "Not a plural",
          ),
        isPlural = true,
      ),
    ).andIsBadRequest
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `respects pluralArgName on create`() {
    saveAndPrepare()
    performProjectAuthPost(
      "keys",
      CreateKeyDto(
        name = "new_key",
        pluralArgName = "dogsCount",
        translations =
          mapOf(
            "en" to "{count, plural, one {# dog} other {# dogs}}",
          ),
        isPlural = true,
      ),
    ).andIsCreated.andAssertThatJson {
      node("pluralArgName").isEqualTo("dogsCount")
      node("translations.en.text").isString.isEqualTo("{dogsCount, plural,\none {# dog}\nother {# dogs}\n}")
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `correctly imports with non-resolvable endpoint`() {
    testData.projectBuilder.addCzech()
    saveAndPrepare()
    performProjectAuthPost(
      "keys/import",
      mapOf(
        "keys" to
          listOf(
            mapOf(
              "name" to "plural_key",
              "translations" to
                mapOf(
                  "en" to "Hello! I have {count} dogs.",
                  "cs" to "Ahoj! Já mám {count, plural, one {jednoho psa} few {# psi} other {# psů}}",
                ),
            ),
            mapOf(
              "name" to "not_plural_key",
              "translations" to
                mapOf(
                  "en" to "Hello!",
                  "cs" to "Ahoj!",
                ),
            ),
          ),
      ),
    ).andIsOk

    executeInNewTransaction {
      val pluralKey = keyService.find(testData.project.id, "plural_key", null)
      pluralKey!!.isPlural.assert.isTrue()
      pluralKey.translations
        .find { it.language.tag == "en" }!!
        .text.assert
        .isEqualTo("{count, plural,\nother {Hello! I have {count} dogs.}\n}")
      pluralKey.translations
        .find { it.language.tag == "cs" }!!
        .text.assert
        .isEqualTo(
          "{count, plural,\n" +
            "one {Ahoj! Já mám jednoho psa}\n" +
            "few {Ahoj! Já mám # psi}\n" +
            "other {Ahoj! Já mám # psů}\n" +
            "}",
        )

      val notPluralKey = keyService.find(testData.project.id, "not_plural_key", null)
      notPluralKey!!.isPlural.assert.isFalse()
      notPluralKey.translations
        .find { it.language.tag == "en" }!!
        .text.assert
        .isEqualTo("Hello!")
      notPluralKey.translations
        .find { it.language.tag == "cs" }!!
        .text.assert
        .isEqualTo("Ahoj!")
    }
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `correctly imports with resolvable endpoint`() {
    testData.projectBuilder.addCzech()
    testData.projectBuilder
      .addKey {
        name = "existing_non_plural"
      }.build {
        addTranslation("en", "Hello!")
      }
    saveAndPrepare()
    performProjectAuthPost(
      "keys/import-resolvable",
      mapOf(
        "keys" to
          listOf(
            mapOf(
              "name" to "plural_key",
              "translations" to
                mapOf(
                  "en" to
                    mapOf(
                      "text" to "Hello! I have {count} dogs.",
                      "resolution" to "NEW",
                    ),
                  "cs" to
                    mapOf(
                      "text" to "Ahoj! Já mám {count, plural, one {jednoho psa} few {# psi} other {# psů}}",
                      "resolution" to "NEW",
                    ),
                ),
            ),
            mapOf(
              "name" to "not_plural_key",
              "translations" to
                mapOf(
                  "en" to
                    mapOf(
                      "text" to "Hello!",
                      "resolution" to "NEW",
                    ),
                  "cs" to
                    mapOf(
                      "text" to "Ahoj!",
                      "resolution" to "NEW",
                    ),
                ),
            ),
            mapOf(
              "name" to "existing_non_plural",
              "translations" to
                mapOf(
                  "cs" to
                    mapOf(
                      "text" to "{hello, plural, one {# pes} other {# psů}}",
                      "resolution" to "NEW",
                    ),
                ),
            ),
          ),
      ),
    ).andIsOk

    executeInNewTransaction {
      val pluralKey = keyService.find(testData.project.id, "plural_key", null)
      pluralKey!!.isPlural.assert.isTrue()
      pluralKey.translations
        .find { it.language.tag == "en" }!!
        .text.assert
        .isEqualTo("{count, plural,\nother {Hello! I have {count} dogs.}\n}")
      pluralKey.translations
        .find { it.language.tag == "cs" }!!
        .text.assert
        .isEqualTo(
          "{count, plural,\n" +
            "one {Ahoj! Já mám jednoho psa}\n" +
            "few {Ahoj! Já mám # psi}\n" +
            "other {Ahoj! Já mám # psů}\n" +
            "}",
        )

      val notPluralKey = keyService.find(testData.project.id, "not_plural_key", null)
      notPluralKey!!.isPlural.assert.isFalse()
      notPluralKey.translations
        .find { it.language.tag == "en" }!!
        .text.assert
        .isEqualTo("Hello!")
      notPluralKey.translations
        .find { it.language.tag == "cs" }!!
        .text.assert
        .isEqualTo("Ahoj!")

      val existingNonPluralKey = keyService.find(testData.project.id, "existing_non_plural", null)
      existingNonPluralKey!!.isPlural.assert.isTrue()
      existingNonPluralKey.translations
        .find { it.language.tag == "en" }!!
        .text.assert
        .isEqualTo("{hello, plural,\nother {Hello!}\n}")
      existingNonPluralKey.translations
        .find { it.language.tag == "cs" }!!
        .text.assert
        .isEqualTo("{hello, plural,\none {# pes}\nother {# psů}\n}")
    }
  }
}
