package io.tolgee.api.v2.controllers.v2KeyController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.KeysTestData
import io.tolgee.dtos.request.key.ComplexEditKeyDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.model.enums.Scope
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.util.generateImage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
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

  @Autowired
  lateinit var bigMetaService: BigMetaService

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
      node("params[0][0]").isEqualTo("Not a plural")
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
      node("translations.en.text").isString.isEqualTo("{value, plural, other {Oh}}")
      node("translations.de.text").isString.isEqualTo(NORMALIZED_PLURAL)
      node("translations.cs.text").isString.isEqualTo(NORMALIZED_PLURAL)
    }
  }
}
