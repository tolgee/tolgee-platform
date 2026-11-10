package io.tolgee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.CommunityStatsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date

class CommunityStatsControllerTest : AuthorizedControllerTest() {
  private lateinit var testData: CommunityStatsTestData

  private val wednesday = LocalDate.of(2026, 3, 4)
  private val sundayOfPreviousIsoWeek = LocalDate.of(2026, 2, 22)
  private val activity get() = "/v2/projects/${testData.publicProject.id}/community/activity"

  @BeforeEach
  fun setup() {
    currentDateProvider.forcedDate = Date.from(wednesday.atTime(12, 0).toInstant(ZoneOffset.UTC))
    testData = CommunityStatsTestData()
    testDataService.saveTestData(testData.root)
    executeInNewTransaction { testData.replayActivity(entityManager) }
    executeInNewTransaction {
      testData.moveCounterDays(entityManager, testData.bob, testData.publicProject, sundayOfPreviousIsoWeek)
    }
    userAccount = testData.carol
  }

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `activity returns zero-filled buckets ending with the current week and live tile counts`() {
    performAuthGet(activity)
      .andIsOk
      .andAssertThatJson {
        node("buckets") {
          isArray.hasSize(12)
          node("[0].start").isEqualTo("2025-12-15")
          node("[9].start").isEqualTo("2026-02-16")
          node("[9].contributions").isEqualTo(2)
          node("[9].suggested").isEqualTo(2)
          node("[9].accepted").isEqualTo(1)
          node("[9].declined").isEqualTo(0)
          node("[10].contributions").isEqualTo(0)
          node("[11].start").isEqualTo("2026-03-02")
          node("[11].contributions").isEqualTo(11)
          node("[11].suggested").isEqualTo(4)
          node("[11].accepted").isEqualTo(2)
          node("[11].declined").isEqualTo(1)
        }
        node("translationsWithAnySuggestion").isEqualTo(4)
        node("translationsWithPendingSuggestion").isEqualTo(2)
      }
  }

  @Test
  fun `activity respects range, daily granularity and language filter, tiles included`() {
    performAuthGet("$activity?from=2026-02-16&to=2026-02-22&granularity=DAY")
      .andIsOk
      .andAssertThatJson {
        node("buckets") {
          isArray.hasSize(7)
          node("[0].start").isEqualTo("2026-02-16")
          node("[6].start").isEqualTo("2026-02-22")
          node("[6].contributions").isEqualTo(2)
        }
        node("translationsWithAnySuggestion").isEqualTo(0)
        node("translationsWithPendingSuggestion").isEqualTo(0)
      }
    performAuthGet("$activity?languageIds=${testData.german.id}")
      .andIsOk
      .andAssertThatJson {
        node("buckets[11].contributions").isEqualTo(2)
        node("buckets[11].suggested").isEqualTo(1)
        node("buckets[11].declined").isEqualTo(1)
        node("translationsWithAnySuggestion").isEqualTo(1)
        node("translationsWithPendingSuggestion").isEqualTo(0)
      }
  }

  @Test
  fun `the range is capped at 2 years and a from after to collapses to one bucket`() {
    performAuthGet("$activity?from=2000-01-01")
      .andIsOk
      .andAssertThatJson {
        node("buckets[0].start").isEqualTo("2024-03-04")
        node("buckets").isArray.hasSize(105)
      }
    performAuthGet("$activity?from=2026-03-10&to=2026-03-04&granularity=DAY")
      .andIsOk
      .andAssertThatJson {
        node("buckets").isArray.hasSize(1)
        node("buckets[0].start").isEqualTo("2026-03-04")
      }
  }
}
