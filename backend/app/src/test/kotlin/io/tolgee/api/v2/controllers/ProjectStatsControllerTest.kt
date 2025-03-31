package io.tolgee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.TranslationsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.fixtures.isValidId
import io.tolgee.fixtures.node
import io.tolgee.testing.annotations.ProjectApiKeyAuthTestMethod
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.text.SimpleDateFormat

class ProjectStatsControllerTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: TranslationsTestData

  private var activityCounter = 0

  var format: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd")

  @BeforeEach
  fun setup() {
    mockDate("2022-03-20")
    testData = TranslationsTestData()
    testData.addTranslationsWithStates()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    projectSupplier = { testData.projectBuilder.self }
  }

  @AfterEach
  fun clear() {
    clearForcedDate()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns stats`() {
    performProjectAuthGet("stats").andIsOk.andPrettyPrint.andAssertThatJson {
      node("projectId").isNumber
      node("languageCount").isEqualTo(2)
      node("keyCount").isEqualTo(8)
      node("baseWordsCount").isEqualTo(7)
      node("translatedPercentage").isNumber.isLessThan(BigDecimal(14.5)).isGreaterThan(BigDecimal(14))
      node("reviewedPercentage").isNumber.isLessThan(BigDecimal(14.5)).isGreaterThan(BigDecimal(14))
      node("membersCount").isEqualTo(1)
      node("tagCount").isEqualTo(3)
      node("languageStats") {
        isArray
        node("[0]") {
          node("languageId").isValidId
          node("languageTag").isEqualTo("en")
          node("languageName").isEqualTo("English")
          node("languageOriginalName").isEqualTo("English")
          node("languageFlagEmoji").isEqualTo(null)
          node("translatedKeyCount").isEqualTo(3)
          node("translatedWordCount").isEqualTo(4)
          node("translatedPercentage").isEqualTo(57.14285714285714)
          node("reviewedKeyCount").isEqualTo(3)
          node("reviewedWordCount").isEqualTo(3)
          node("reviewedPercentage").isEqualTo(42.857142857142854)
          node("translationsUpdatedAt").isNotNull
        }
        node("[1]") {
          node("languageId").isValidId
          node("languageTag").isEqualTo("de")
          node("languageName").isEqualTo("German")
          node("languageOriginalName").isEqualTo("Deutsch")
          node("languageFlagEmoji").isEqualTo(null)
          node("translatedKeyCount").isEqualTo(1)
          node("translatedWordCount").isEqualTo(1)
          node("translatedPercentage").isEqualTo(14.285714285714285)
          node("reviewedKeyCount").isEqualTo(2)
          node("reviewedWordCount").isEqualTo(1)
          node("reviewedPercentage").isEqualTo(14.285714285714285)
          node("untranslatedKeyCount").isEqualTo(5)
          node("untranslatedWordCount").isEqualTo(5)
          node("untranslatedPercentage").isEqualTo(71.42857142857143)
          node("translationsUpdatedAt").isNotNull
        }
      }
    }
  }

  @ProjectApiKeyAuthTestMethod
  fun `returns stats with API key`() {
    `returns stats`()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `returns daily activity`() {
    mockDate("2022-04-01")
    createActivity(1)
    mockDate("2022-04-05")
    createActivity(5)
    mockDate("2022-04-20")
    createActivity(2)
    performProjectAuthGet("stats/daily-activity").andIsOk.andAssertThatJson {
      isEqualTo(
        """
      {
        "2022-03-20" : 1,
        "2022-04-01" : 1,
        "2022-04-05" : 5,
        "2022-04-20" : 2
      }
      """,
      )
    }
  }

  private fun mockDate(stringDate: String) {
    setForcedDate(format.parse(stringDate))
  }

  private fun createActivity(times: Int) {
    repeat(times) {
      performProjectAuthPut(
        "translations",
        mapOf("key" to "A key", "translations" to mapOf(testData.englishLanguage.tag to "A key ${activityCounter++}")),
      ).andIsOk
    }
  }
}
