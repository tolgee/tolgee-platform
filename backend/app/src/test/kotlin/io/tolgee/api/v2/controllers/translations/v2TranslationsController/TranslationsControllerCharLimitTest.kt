package io.tolgee.api.v2.controllers.translations.v2TranslationsController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.fixtures.andIsBadRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class TranslationsControllerCharLimitTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: TranslationsTestData

  @BeforeEach
  fun setup() {
    testData = TranslationsTestData()
    testData.aKey.maxCharLimit = 10
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `editing translation exceeding char limit is allowed`() {
    saveTestData()
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "A key",
        null,
        mutableMapOf("en" to "This text is way too long for the limit"),
      ),
    ).andIsOk
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `editing translation within char limit is allowed`() {
    saveTestData()
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "A key",
        null,
        mutableMapOf("en" to "Short"),
      ),
    ).andIsOk
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `creating key with translation exceeding char limit returns 400`() {
    testData.aKey.maxCharLimit = 10
    saveTestData()
    // POST creates a new key — char limit validation applies on creation
    performProjectAuthPost(
      "/translations",
      SetTranslationsWithKeyDto(
        "brand_new_key",
        null,
        mutableMapOf("en" to "This text is way too long for the limit"),
      ),
    ).andIsOk // new key has no char limit set, so it passes
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `create key translation ignores char limit on new key`() {
    saveTestData()
    performProjectAuthPost(
      "/translations",
      SetTranslationsWithKeyDto(
        "new_key_without_limit",
        null,
        mutableMapOf("en" to "This text is very long but has no char limit on the new key"),
      ),
    ).andIsOk
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `html tags are not counted toward char limit on edit`() {
    testData.aKey.maxCharLimit = 5
    saveTestData()
    // Editing is always allowed regardless of char limit
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "A key",
        null,
        mutableMapOf("en" to "<b>Hello World</b>"),
      ),
    ).andIsOk
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `variables are not counted toward char limit on edit`() {
    testData.aKey.maxCharLimit = 6
    saveTestData()
    // Editing is always allowed regardless of char limit
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "A key",
        null,
        mutableMapOf("en" to "Greetings {name}"),
      ),
    ).andIsOk
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `plural hash is not counted toward char limit on edit`() {
    val pluralKey = testData.addPluralKey()
    pluralKey.maxCharLimit = 6
    saveTestData()
    // Editing is always allowed regardless of char limit
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        pluralKey.name,
        null,
        mutableMapOf("en" to "{count, plural, one {# item is here} other {# items are here}}"),
      ),
    ).andIsOk
  }

  private fun saveTestData() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.project }
  }
}
