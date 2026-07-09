package io.tolgee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.AutoTranslateTestData
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.fixtures.MachineTranslationTest
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ContextRecreatingTest
class AiMatchStatsControllerTest : MachineTranslationTest() {
  companion object {
    private const val GOOGLE_TEXT = "Translated with Google"
    private const val KEY = "base-translation-doesn't-exist"
  }

  private lateinit var testData: AutoTranslateTestData

  @BeforeEach
  fun setup() {
    testData = AutoTranslateTestData()
    projectSupplier = { testData.project }
    initMachineTranslationMocks()
    initMachineTranslationProperties(150000L)
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @AfterEach
  fun clear() {
    clearForcedDate()
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `scores a verbatim-approved AI translation as a 100 percent match`() {
    mockDate("2026-01-01")
    val deId = autoTranslateGerman()
    mockDate("2026-02-01")
    review(deId)

    performProjectAuthGet("ai-match-stats").andIsOk.andAssertThatJson {
      node("avgMatchScore").isEqualTo(100)
      node("reviewedPct").isEqualTo(100)
      node("reviewedKeys").isEqualTo(1)
      node("reviewedWords").isEqualTo(3)
      node("b100.words").isEqualTo(3)
      node("notReviewedWords").isEqualTo(0)
      node("langCount").isEqualTo(1)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `recomputes the score when the reviewer edits the AI text`() {
    mockDate("2026-01-01")
    val deId = autoTranslateGerman()
    mockDate("2026-02-01")
    review(deId)

    // reviewer rewrites it entirely, then re-reviews -> aiText reconstructed from history, score drops
    mockDate("2026-03-01")
    performProjectAuthPut(
      "translations",
      SetTranslationsWithKeyDto(key = KEY, translations = mapOf("de" to "totally different words here now")),
    ).andIsOk
    mockDate("2026-03-02")
    review(deId)

    performProjectAuthGet("ai-match-stats").andIsOk.andAssertThatJson {
      node("reviewedKeys").isEqualTo(1)
      node("b100.words").isEqualTo(0)
      node("bno.words").isEqualTo(5)
      node("avgMatchScore").isEqualTo(0)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `counts an unreviewed AI cell as not reviewed`() {
    mockDate("2026-01-01")
    autoTranslateGerman()

    performProjectAuthGet("ai-match-stats").andIsOk.andAssertThatJson {
      node("reviewedWords").isEqualTo(0)
      node("notReviewedWords").isEqualTo(3)
      node("reviewedPct").isEqualTo(0)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `groups the MT engine as its own provider in per-prompt stats`() {
    mockDate("2026-01-01")
    val deId = autoTranslateGerman()
    mockDate("2026-02-01")
    review(deId)

    performProjectAuthGet("ai-match-stats/prompts").andIsOk.andAssertThatJson {
      node("perPrompt").isArray.hasSize(1)
      node("perPrompt[0].provider").isEqualTo("GOOGLE")
      node("perPrompt[0].promptId").isNull()
      node("perPrompt[0].versionLabel").isNull()
      node("perPrompt[0].avgMatchScore").isEqualTo(100)
      node("perPrompt[0].reviewedWords").isEqualTo(3)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `reports not-yet-reviewed AI cells per prompt`() {
    mockDate("2026-01-01")
    autoTranslateGerman() // AI-translated, left unreviewed

    performProjectAuthGet("ai-match-stats/prompts").andIsOk.andAssertThatJson {
      node("perPrompt").isArray.hasSize(1)
      node("perPrompt[0].provider").isEqualTo("GOOGLE")
      node("perPrompt[0].reviewedWords").isEqualTo(0)
      node("perPrompt[0].notReviewed").isEqualTo(3)
      node("perPrompt[0].notReviewedKeys").isEqualTo(1)
      node("perPrompt[0].total").isEqualTo(3)
      node("perPrompt[0].notReviewedPct").isEqualTo(100.0)
      node("perPrompt[0].versionLabel").isNull()
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `applies the reviewedAfter and reviewedBefore range filter`() {
    mockDate("2026-01-01")
    val deId = autoTranslateGerman()
    mockDate("2026-02-01")
    review(deId)

    // window after the review -> excluded
    performProjectAuthGet("ai-match-stats?reviewedAfter=${date("2026-03-01").time}").andIsOk.andAssertThatJson {
      node("reviewedKeys").isEqualTo(0)
      node("b100.words").isEqualTo(0)
    }
    // window before the review -> excluded
    performProjectAuthGet("ai-match-stats?reviewedBefore=${date("2026-01-15").time}").andIsOk.andAssertThatJson {
      node("reviewedKeys").isEqualTo(0)
    }
    // window containing the review -> included
    performProjectAuthGet("ai-match-stats?reviewedAfter=${date("2026-01-15").time}").andIsOk.andAssertThatJson {
      node("reviewedKeys").isEqualTo(1)
      node("b100.words").isEqualTo(3)
    }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `reports the reviewed German translation in the per-language breakdown`() {
    mockDate("2026-01-01")
    val deId = autoTranslateGerman()
    mockDate("2026-02-01")
    review(deId)

    performProjectAuthGet("ai-match-stats/languages").andIsOk.andAssertThatJson {
      node("perLang").isArray.hasSize(1)
      node("perLang[0].tag").isEqualTo("de")
      node("perLang[0].b100").isEqualTo(3)
      node("perLang[0].total").isEqualTo(3)
      node("perLang[0].avgMatchScore").isEqualTo(100)
      node("perLang[0].b100Pct").isEqualTo(100.0)
    }
  }

  /** Sets the English base for [KEY], which auto-translates German via the mocked Google engine. */
  private fun autoTranslateGerman(): Long {
    performProjectAuthPut(
      "translations",
      SetTranslationsWithKeyDto(key = KEY, translations = mapOf("en" to "Hello")),
    ).andIsOk
    waitForNotThrowing {
      executeInNewTransaction {
        val de = testData.baseTranslationNotExistKey.getLangTranslation(testData.germanLanguage)
        de.text.assert.isEqualTo(GOOGLE_TEXT)
        de.auto.assert.isTrue()
      }
    }
    return executeInNewTransaction {
      testData.baseTranslationNotExistKey.getLangTranslation(testData.germanLanguage).id
    }
  }

  private fun review(translationId: Long) {
    performProjectAuthPut("translations/$translationId/set-state/REVIEWED").andIsOk
  }

  private fun mockDate(stringDate: String) {
    setForcedDate(date(stringDate))
  }

  private fun date(stringDate: String): Date =
    SimpleDateFormat("yyyy-MM-dd").apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(stringDate)
}
