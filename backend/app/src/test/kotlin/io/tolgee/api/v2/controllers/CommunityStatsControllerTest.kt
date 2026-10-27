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
  fun `contributors carry counts, dates, languages and weeks, most contributions first, disabled users excluded`() {
    performAuthGet("/v2/projects/${testData.publicProject.id}/community/contributors?weeks=3")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors") {
          isArray.hasSize(3)
          node("[0].id").isEqualTo(testData.user.id)
          node("[0].contributions").isEqualTo(6)
          node("[0].suggested").isEqualTo(0)
          node("[1].id").isEqualTo(testData.alice.id)
          node("[1].name").isEqualTo("Alice Adams")
          node("[1].username").isAbsent()
          node("[1].contributions").isEqualTo(5)
          node("[1].suggested").isEqualTo(4)
          node("[1].accepted").isEqualTo(2)
          node("[1].declined").isEqualTo(1)
          node("[1].firstContributionAt").isEqualTo(currentDateProvider.date.time)
          node("[1].lastContributionAt").isEqualTo(currentDateProvider.date.time)
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
          node("[1].weeks") {
            isArray.hasSize(3)
            node("[0].weekStart").isEqualTo("2026-02-16")
            node("[0].contributions").isEqualTo(0)
            node("[2].weekStart").isEqualTo("2026-03-02")
            node("[2].contributions").isEqualTo(5)
            node("[2].suggested").isEqualTo(4)
            node("[2].accepted").isEqualTo(2)
            node("[2].declined").isEqualTo(1)
          }
          node("[2].id").isEqualTo(testData.bob.id)
          node("[2].contributions").isEqualTo(2)
          node("[2].suggested").isEqualTo(2)
          node("[2].accepted").isEqualTo(1)
          node("[2].weeks[0].weekStart").isEqualTo("2026-02-16")
          node("[2].weeks[0].contributions").isEqualTo(2)
          node("[2].weeks[2].contributions").isEqualTo(0)
        }
        node("page.totalElements").isEqualTo(3)
      }
  }

  @Test
  fun `contributors can be sorted by another counter`() {
    performAuthGet("/v2/projects/${testData.publicProject.id}/community/contributors?sort=accepted,desc")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].id").isEqualTo(testData.alice.id)
      }
  }

  @Test
  fun `an unknown sort property falls back to the default order`() {
    performAuthGet("/v2/projects/${testData.publicProject.id}/community/contributors?sort=name,asc")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].id").isEqualTo(testData.user.id)
      }
  }

  @Test
  fun `a single contributor carries the same totals, languages and weeks as the list`() {
    performAuthGet("/v2/projects/${testData.publicProject.id}/community/contributors/${testData.alice.id}?weeks=3")
      .andIsOk
      .andAssertThatJson {
        node("id").isEqualTo(testData.alice.id)
        node("name").isEqualTo("Alice Adams")
        node("contributions").isEqualTo(5)
        node("suggested").isEqualTo(4)
        node("accepted").isEqualTo(2)
        node("declined").isEqualTo(1)
        node("firstContributionAt").isEqualTo(currentDateProvider.date.time)
        node("lastContributionAt").isEqualTo(currentDateProvider.date.time)
        node("languages") {
          isArray.hasSize(2)
          node("[0].languageId").isEqualTo(testData.czech.id)
          node("[0].contributions").isEqualTo(4)
          node("[0].suggested").isEqualTo(3)
        }
        node("weeks") {
          isArray.hasSize(3)
          node("[2].weekStart").isEqualTo("2026-03-02")
          node("[2].contributions").isEqualTo(5)
          node("[2].suggested").isEqualTo(4)
        }
      }
  }

  @Test
  fun `a user the list does not show is not found`() {
    val base = "/v2/projects/${testData.publicProject.id}/community/contributors"
    performAuthGet("$base/${testData.carol.id}").andIsNotFound
    performAuthGet("$base/${testData.dave.id}").andIsNotFound
    performAuthGet("$base/999999999").andIsNotFound
  }

  @Test
  fun `a single contributor of a private project is hidden from a non-member`() {
    userAccount = testData.bob
    performAuthGet("/v2/projects/${testData.privateProject.id}/community/contributors/${testData.alice.id}")
      .andIsNotFound
  }

  @Test
  fun `a private project is hidden from a non-member`() {
    userAccount = testData.bob
    performAuthGet("/v2/projects/${testData.privateProject.id}/community/contributors").andIsNotFound
  }

  @Test
  fun `activity returns zero-filled weekly buckets ending with the current week`() {
    performAuthGet("/v2/projects/${testData.publicProject.id}/community/activity?weeks=3")
      .andIsOk
      .andAssertThatJson {
        node("weeks") {
          isArray.hasSize(3)
          node("[0].weekStart").isEqualTo("2026-02-16")
          node("[0].contributions").isEqualTo(2)
          node("[0].suggested").isEqualTo(2)
          node("[0].accepted").isEqualTo(1)
          node("[0].declined").isEqualTo(0)
          node("[1].weekStart").isEqualTo("2026-02-23")
          node("[1].contributions").isEqualTo(0)
          node("[2].weekStart").isEqualTo("2026-03-02")
          node("[2].contributions").isEqualTo(11)
          node("[2].suggested").isEqualTo(4)
          node("[2].accepted").isEqualTo(2)
          node("[2].declined").isEqualTo(1)
        }
      }
  }

  @Test
  fun `a soft-deleted language drops out of totals, languages and activity alike`() {
    jdbcTemplate.update("update language set deleted_at = now() where id = ?", testData.german.id)
    performAuthGet("/v2/projects/${testData.publicProject.id}/community/contributors")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].id").isEqualTo(testData.user.id)
        node("_embedded.contributors[0].contributions").isEqualTo(5)
        node("_embedded.contributors[1].id").isEqualTo(testData.alice.id)
        node("_embedded.contributors[1].contributions").isEqualTo(4)
        node("_embedded.contributors[1].suggested").isEqualTo(3)
        node("_embedded.contributors[1].declined").isEqualTo(0)
        node("_embedded.contributors[1].languages").isArray.hasSize(1)
      }
    performAuthGet("/v2/projects/${testData.publicProject.id}/community/contributors/${testData.alice.id}")
      .andIsOk
      .andAssertThatJson {
        node("contributions").isEqualTo(4)
        node("suggested").isEqualTo(3)
        node("languages").isArray.hasSize(1)
      }
    performAuthGet("/v2/projects/${testData.publicProject.id}/community/activity?weeks=1")
      .andIsOk
      .andAssertThatJson {
        node("weeks[0].contributions").isEqualTo(9)
        node("weeks[0].suggested").isEqualTo(3)
        node("weeks[0].accepted").isEqualTo(2)
        node("weeks[0].declined").isEqualTo(0)
      }
  }

  @Test
  fun `weeks is clamped to 1 to 104`() {
    performAuthGet("/v2/projects/${testData.publicProject.id}/community/activity?weeks=0")
      .andIsOk
      .andAssertThatJson { node("weeks").isArray.hasSize(1) }
    performAuthGet("/v2/projects/${testData.publicProject.id}/community/activity?weeks=99999")
      .andIsOk
      .andAssertThatJson { node("weeks").isArray.hasSize(104) }
  }
}
