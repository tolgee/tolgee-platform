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
  fun `set translation exceeding char limit returns 400`() {
    saveTestData()
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "A key",
        null,
        mutableMapOf("en" to "This text is way too long for the limit"),
      ),
    ).andIsBadRequest
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `set translation within char limit returns 200`() {
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
  fun `html tags are not counted toward char limit`() {
    testData.aKey.maxCharLimit = 5
    saveTestData()
    // "<b>Hello</b>" has 5 visible chars ("Hello"), should pass with limit 5
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "A key",
        null,
        mutableMapOf("en" to "<b>Hello</b>"),
      ),
    ).andIsOk

    // "<b>Hello World</b>" has 11 visible chars, should fail with limit 5
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "A key",
        null,
        mutableMapOf("en" to "<b>Hello World</b>"),
      ),
    ).andIsBadRequest
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `variables are not counted toward char limit`() {
    testData.aKey.maxCharLimit = 6
    saveTestData()
    // "Hello {name}" has 6 visible chars ("Hello "), should pass with limit 6
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "A key",
        null,
        mutableMapOf("en" to "Hello {name}"),
      ),
    ).andIsOk

    // "Greetings {name}" has 10 visible chars ("Greetings "), should fail with limit 6
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        "A key",
        null,
        mutableMapOf("en" to "Greetings {name}"),
      ),
    ).andIsBadRequest
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `plural hash is not counted toward char limit`() {
    val pluralKey = testData.addPluralKey()
    pluralKey.maxCharLimit = 6
    saveTestData()
    // "{count, plural, one {# item} other {# items}}" — form texts are "# item" (5 visible) and "# items" (6 visible)
    // Both within limit of 6
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        pluralKey.name,
        null,
        mutableMapOf("en" to "{count, plural, one {# item} other {# items}}"),
      ),
    ).andIsOk

    // "{count, plural, one {# item is here} other {# items are here}}" — "# items are here" → 15 visible chars
    // Should fail with limit 6
    performProjectAuthPut(
      "/translations",
      SetTranslationsWithKeyDto(
        pluralKey.name,
        null,
        mutableMapOf("en" to "{count, plural, one {# item is here} other {# items are here}}"),
      ),
    ).andIsBadRequest
  }

  private fun saveTestData() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.project }
  }
}
