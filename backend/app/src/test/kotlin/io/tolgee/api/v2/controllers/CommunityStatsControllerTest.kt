package io.tolgee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.CommunityStatsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsNotFound
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.node
import io.tolgee.testing.AuthorizedControllerTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date

class CommunityStatsControllerTest : AuthorizedControllerTest() {
  private lateinit var testData: CommunityStatsTestData

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  private val wednesday = LocalDate.of(2026, 3, 4)
  private val sundayOfPreviousIsoWeek = LocalDate.of(2026, 2, 22)
  private val contributors get() = "/v2/projects/${testData.publicProject.id}/community/contributors"
  private val activity get() = "/v2/projects/${testData.publicProject.id}/community/activity"

  @BeforeEach
  fun setup() {
    currentDateProvider.forcedDate = Date.from(wednesday.atTime(12, 0).toInstant(ZoneOffset.UTC))
    testData = CommunityStatsTestData()
    testDataService.saveTestData(testData.root)
    executeInNewTransaction { testData.replayActivity(entityManager) }
    jdbcTemplate.update(
      "update community_contribution_day set day = ? where project_id = ? and user_id = ?",
      sundayOfPreviousIsoWeek,
      testData.publicProject.id,
      testData.bob.id,
    )
    userAccount = testData.carol
  }

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `contributors carry counts, pending in this project only, ISO dates, languages and 12 weekly buckets`() {
    performAuthGet(contributors)
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors") {
          isArray.hasSize(3)
          node("[0].id").isEqualTo(testData.user.id)
          node("[0].contributions").isEqualTo(6)
          node("[0].suggested").isEqualTo(0)
          node("[0].active").isEqualTo(0)
          node("[1].id").isEqualTo(testData.alice.id)
          node("[1].name").isEqualTo("Alice Adams")
          node("[1].username").isAbsent()
          node("[1].contributions").isEqualTo(5)
          node("[1].suggested").isEqualTo(4)
          node("[1].accepted").isEqualTo(2)
          node("[1].declined").isEqualTo(1)
          node("[1].active").isEqualTo(1)
          node("[1].firstContributionAt").isEqualTo("2026-03-04T12:00:00.000Z")
          node("[1].lastContributionAt").isEqualTo("2026-03-04T12:00:00.000Z")
          node("[1].languages") {
            isArray.hasSize(2)
            node("[0].languageId").isEqualTo(testData.czech.id)
            node("[0].contributions").isEqualTo(4)
            node("[0].suggested").isEqualTo(3)
            node("[0].accepted").isEqualTo(2)
            node("[0].declined").isEqualTo(0)
            node("[1].languageId").isEqualTo(testData.german.id)
            node("[1].contributions").isEqualTo(1)
            node("[1].declined").isEqualTo(1)
          }
          node("[1].weeks").isAbsent()
          node("[1].buckets") {
            isArray.hasSize(12)
            node("[0].start").isEqualTo("2025-12-15")
            node("[0].contributions").isEqualTo(0)
            node("[11].start").isEqualTo("2026-03-02")
            node("[11].contributions").isEqualTo(5)
            node("[11].suggested").isEqualTo(4)
            node("[11].accepted").isEqualTo(2)
            node("[11].declined").isEqualTo(1)
          }
          node("[2].id").isEqualTo(testData.bob.id)
          node("[2].contributions").isEqualTo(2)
          node("[2].suggested").isEqualTo(2)
          node("[2].accepted").isEqualTo(1)
          node("[2].active").isEqualTo(1)
          node("[2].buckets[9].start").isEqualTo("2026-02-16")
          node("[2].buckets[9].contributions").isEqualTo(2)
          node("[2].buckets[11].contributions").isEqualTo(0)
        }
        node("page.totalElements").isEqualTo(3)
      }
  }

  @Test
  fun `range, daily granularity and language filter narrow totals, languages, buckets and the ranking alike`() {
    performAuthGet(
      "$contributors?from=2026-03-01&to=2026-03-04&granularity=DAY&languageIds=${testData.czech.id}",
    ).andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(2)
        node("_embedded.contributors") {
          isArray.hasSize(2)
          node("[0].id").isEqualTo(testData.user.id)
          node("[0].contributions").isEqualTo(5)
          node("[1].id").isEqualTo(testData.alice.id)
          node("[1].contributions").isEqualTo(4)
          node("[1].suggested").isEqualTo(3)
          node("[1].accepted").isEqualTo(2)
          node("[1].declined").isEqualTo(0)
          node("[1].active").isEqualTo(1)
          node("[1].languages").isArray.hasSize(1)
          node("[1].buckets") {
            isArray.hasSize(4)
            node("[0].start").isEqualTo("2026-03-01")
            node("[2].contributions").isEqualTo(0)
            node("[3].start").isEqualTo("2026-03-04")
            node("[3].contributions").isEqualTo(4)
            node("[3].suggested").isEqualTo(3)
          }
        }
      }
    performAuthGet("$contributors?languageIds=${testData.german.id}")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors") {
          isArray.hasSize(2)
          node("[0].id").isEqualTo(testData.alice.id)
          node("[0].contributions").isEqualTo(1)
          node("[0].suggested").isEqualTo(1)
          node("[0].active").isEqualTo(0)
          node("[1].id").isEqualTo(testData.user.id)
          node("[1].contributions").isEqualTo(1)
        }
      }
  }

  @Test
  fun `contributors sort by pending count and by last contribution date, nulls last`() {
    performAuthGet("$contributors?sort=active,desc")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].active").isEqualTo(1)
        node("_embedded.contributors[1].active").isEqualTo(1)
        node("_embedded.contributors[2].id").isEqualTo(testData.user.id)
      }
    jdbcTemplate.update(
      "update project_contributor set last_contribution_at = ? where project_id = ? and user_id = ?",
      Date.from(sundayOfPreviousIsoWeek.atStartOfDay().toInstant(ZoneOffset.UTC)),
      testData.publicProject.id,
      testData.bob.id,
    )
    jdbcTemplate.update(
      "delete from project_contributor where project_id = ? and user_id = ?",
      testData.publicProject.id,
      testData.user.id,
    )
    performAuthGet("$contributors?sort=lastContributionAt,desc")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].id").isEqualTo(testData.alice.id)
        node("_embedded.contributors[0].lastContributionAt").isEqualTo("2026-03-04T12:00:00.000Z")
        node("_embedded.contributors[1].id").isEqualTo(testData.bob.id)
        node("_embedded.contributors[1].lastContributionAt").isEqualTo("2026-02-22T00:00:00.000Z")
        node("_embedded.contributors[2].id").isEqualTo(testData.user.id)
        node("_embedded.contributors[2].lastContributionAt").isNull()
      }
  }

  @Test
  fun `contributors can be sorted by another counter`() {
    performAuthGet("$contributors?sort=accepted,desc")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].id").isEqualTo(testData.alice.id)
      }
  }

  @Test
  fun `excludeEmpty leaves out contributors without a suggestion in the range`() {
    performAuthGet("$contributors?excludeEmpty=true")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(2)
        node("_embedded.contributors") {
          isArray.hasSize(2)
          node("[0].id").isEqualTo(testData.alice.id)
          node("[1].id").isEqualTo(testData.bob.id)
        }
      }
    performAuthGet("$contributors?excludeEmpty=true&from=2026-03-01")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(1)
        node("_embedded.contributors[0].id").isEqualTo(testData.alice.id)
      }
  }

  @Test
  fun `an unknown sort property falls back to the default order`() {
    performAuthGet("$contributors?sort=name,asc")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].id").isEqualTo(testData.user.id)
      }
  }

  @Test
  fun `a private project is hidden from a non-member`() {
    userAccount = testData.bob
    performAuthGet("/v2/projects/${testData.privateProject.id}/community/contributors").andIsNotFound
  }

  @Test
  fun `activity returns zero-filled buckets ending with the current week and live tile counts`() {
    performAuthGet(activity)
      .andIsOk
      .andAssertThatJson {
        node("weeks").isAbsent()
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

  @Test
  fun `a soft-deleted language drops out of totals, languages and activity alike`() {
    jdbcTemplate.update("update language set deleted_at = now() where id = ?", testData.german.id)
    performAuthGet(contributors)
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].id").isEqualTo(testData.user.id)
        node("_embedded.contributors[0].contributions").isEqualTo(5)
        node("_embedded.contributors[1].id").isEqualTo(testData.alice.id)
        node("_embedded.contributors[1].contributions").isEqualTo(4)
        node("_embedded.contributors[1].suggested").isEqualTo(3)
        node("_embedded.contributors[1].declined").isEqualTo(0)
        node("_embedded.contributors[1].active").isEqualTo(1)
        node("_embedded.contributors[1].languages").isArray.hasSize(1)
      }
    jdbcTemplate.update(
      "update translation_suggestion set state = 'ACTIVE' where language_id = ?",
      testData.german.id,
    )
    performAuthGet("$activity?from=2026-03-02")
      .andIsOk
      .andAssertThatJson {
        node("buckets[0].contributions").isEqualTo(9)
        node("buckets[0].suggested").isEqualTo(3)
        node("buckets[0].accepted").isEqualTo(2)
        node("buckets[0].declined").isEqualTo(0)
        node("translationsWithAnySuggestion").isEqualTo(3)
        node("translationsWithPendingSuggestion").isEqualTo(2)
      }
    performAuthGet("$contributors?sort=active,desc")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].active").isEqualTo(1)
      }
  }
}
