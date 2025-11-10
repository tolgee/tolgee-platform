package io.tolgee.api.v2.controllers.translationSuggestionController

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.SuggestionTestData
import io.tolgee.dtos.request.SuggestRequestDto
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

class TranslationSuggestionControllerTmTest : ProjectAuthControllerTest("/v2/projects/") {
  lateinit var testData: SuggestionTestData

  private fun saveTestData() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @BeforeEach
  fun initTestData() {
    testData = SuggestionTestData()
    projectSupplier = { testData.projectBuilder.self }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it suggests from TM with keyId`() {
    saveTestData()
    performAuthPost(
      "/v2/projects/${project.id}/suggest/translation-memory",
      SuggestRequestDto(keyId = testData.thisIsBeautifulKey.id, targetLanguageId = testData.germanLanguage.id),
    ).andIsOk.andPrettyPrint.andAssertThatJson {
      node("_embedded.translationMemoryItems") {
        node("[0]") {
          node("targetText").isEqualTo("Das ist schön")
          node("baseText").isEqualTo("This is beautiful")
          node("keyName").isEqualTo("key 2")
          node("similarity").isEqualTo("0.6296296")
        }
      }
      node("page.totalElements").isEqualTo(1)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it suggests from TM with baseText`() {
    saveTestData()
    performAuthPost(
      "/v2/projects/${project.id}/suggest/translation-memory",
      SuggestRequestDto(baseText = "This is beautiful", targetLanguageId = testData.germanLanguage.id),
    ).andIsOk.andPrettyPrint.andAssertThatJson {
      node("_embedded.translationMemoryItems") {
        node("[0]") {
          node("targetText").isEqualTo("Das ist schön")
          node("baseText").isEqualTo("This is beautiful")
          node("keyName").isEqualTo("key 2")
          node("similarity").isEqualTo("1.0")
        }
      }
      node("page.totalElements").isEqualTo(3)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it suggests from TM only plurals for plural using keyId`() {
    val pluralKeys = testData.addPluralKeys()
    saveTestData()
    performTmSuggestionExpectSingleResult(
      keyId = pluralKeys.truePlural.id,
      expectedResultKeyName = pluralKeys.sameTruePlural.name,
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it suggests from TM only plurals for non plural using keyId`() {
    val pluralKeys = testData.addPluralKeys()
    saveTestData()
    performTmSuggestionExpectSingleResult(
      keyId = pluralKeys.falsePlural.id,
      expectedResultKeyName = pluralKeys.sameFalsePlural.name,
    )
  }

  @ProjectJWTAuthTestMethod
  @Test
  fun `it suggests from TM only plurals for plural using baseText`() {
    val pluralKeys = testData.addPluralKeys()
    saveTestData()
    performTmSuggestionExpectTwoResults(
      baseText =
        testData.projectBuilder
          .getTranslation(pluralKeys.truePlural, testData.englishLanguage.tag)!!
          .text,
      baseIsPlural = true,
      expectedResultValue =
        testData.projectBuilder
          .getTranslation(pluralKeys.truePlural, testData.germanLanguage.tag)!!
          .text!!,
    )
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it suggests from TM only plurals for non plural using baseText`() {
    val pluralKeys = testData.addPluralKeys()
    saveTestData()
    performTmSuggestionExpectTwoResults(
      baseText =
        testData.projectBuilder
          .getTranslation(pluralKeys.falsePlural, testData.englishLanguage.tag)!!
          .text,
      baseIsPlural = false,
      expectedResultValue =
        testData.projectBuilder
          .getTranslation(pluralKeys.falsePlural, testData.germanLanguage.tag)!!
          .text!!,
    )
  }

  private fun performTmSuggestionExpectSingleResult(
    keyId: Long? = null,
    expectedResultKeyName: String,
  ) {
    performAuthPost(
      "/v2/projects/${project.id}/suggest/translation-memory",
      SuggestRequestDto(
        keyId = keyId,
        targetLanguageId = testData.germanLanguage.id,
      ),
    ).andIsOk.andPrettyPrint.andAssertThatJson {
      node("_embedded.translationMemoryItems") {
        node("[0]") {
          node("keyName").isEqualTo(expectedResultKeyName)
        }
      }
      node("page.totalElements").isEqualTo(1)
    }
  }

  private fun performTmSuggestionExpectTwoResults(
    keyId: Long? = null,
    baseText: String? = null,
    baseIsPlural: Boolean? = null,
    expectedResultValue: String,
  ) {
    performAuthPost(
      "/v2/projects/${project.id}/suggest/translation-memory",
      SuggestRequestDto(
        keyId = keyId,
        baseText = baseText,
        isPlural = baseIsPlural ?: false,
        targetLanguageId = testData.germanLanguage.id,
      ),
    ).andIsOk.andPrettyPrint.andAssertThatJson {
      node("_embedded.translationMemoryItems") {
        node("[0]") {
          node("targetText").isString.isEqualTo(expectedResultValue)
        }
      }
      node("page.totalElements").isEqualTo(2)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `it suggests from TM fast enough`() {
    testData.generateLotOfData()
    saveTestData()
    val time =
      measureTimeMillis {
        performAuthPost(
          "/v2/projects/${project.id}/suggest/translation-memory",
          SuggestRequestDto(keyId = testData.beautifulKey.id, targetLanguageId = testData.germanLanguage.id),
        ).andIsOk
      }
    assertThat(time).isLessThan(2000)
  }
}
