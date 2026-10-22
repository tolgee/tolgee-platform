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
  fun `contributors carry totals, languages and weekly buckets, best accepted first, disabled users excluded`() {
    performAuthGet("/v2/projects/${testData.publicProject.id}/community/contributors?weeks=3")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors") {
          isArray.hasSize(2)
          node("[0].id").isEqualTo(testData.alice.id)
          node("[0].name").isEqualTo("Alice Adams")
          node("[0].username").isAbsent()
          node("[0].suggested").isEqualTo(4)
          node("[0].accepted").isEqualTo(2)
          node("[0].declined").isEqualTo(1)
          node("[0].languages") {
            isArray.hasSize(2)
            node("[0].languageId").isEqualTo(testData.czech.id)
            node("[0].suggested").isEqualTo(3)
            node("[0].accepted").isEqualTo(2)
            node("[0].declined").isEqualTo(0)
            node("[1].languageId").isEqualTo(testData.german.id)
            node("[1].declined").isEqualTo(1)
          }
          node("[0].weeks") {
            isArray.hasSize(3)
            node("[0].weekStart").isEqualTo("2026-02-16")
            node("[0].suggested").isEqualTo(0)
            node("[2].weekStart").isEqualTo("2026-03-02")
            node("[2].suggested").isEqualTo(4)
            node("[2].accepted").isEqualTo(2)
            node("[2].declined").isEqualTo(1)
          }
          node("[1].id").isEqualTo(testData.bob.id)
          node("[1].suggested").isEqualTo(2)
          node("[1].accepted").isEqualTo(1)
          node("[1].weeks[0].weekStart").isEqualTo("2026-02-16")
          node("[1].weeks[0].suggested").isEqualTo(2)
          node("[1].weeks[2].suggested").isEqualTo(0)
        }
        node("page.totalElements").isEqualTo(2)
      }
  }

  @Test
  fun `contributors can be sorted by another counter`() {
    performAuthGet("/v2/projects/${testData.publicProject.id}/community/contributors?sort=suggested,asc")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].id").isEqualTo(testData.bob.id)
      }
  }

  @Test
  fun `an unknown sort property falls back to the default order`() {
    performAuthGet("/v2/projects/${testData.publicProject.id}/community/contributors?sort=name,asc")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.contributors[0].id").isEqualTo(testData.alice.id)
      }
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
          node("[0].suggested").isEqualTo(2)
          node("[0].accepted").isEqualTo(1)
          node("[0].declined").isEqualTo(0)
          node("[1].weekStart").isEqualTo("2026-02-23")
          node("[1].suggested").isEqualTo(0)
          node("[2].weekStart").isEqualTo("2026-03-02")
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
        node("_embedded.contributors[0].id").isEqualTo(testData.alice.id)
        node("_embedded.contributors[0].suggested").isEqualTo(3)
        node("_embedded.contributors[0].declined").isEqualTo(0)
        node("_embedded.contributors[0].languages").isArray.hasSize(1)
      }
    performAuthGet("/v2/projects/${testData.publicProject.id}/community/activity?weeks=1")
      .andIsOk
      .andAssertThatJson {
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
