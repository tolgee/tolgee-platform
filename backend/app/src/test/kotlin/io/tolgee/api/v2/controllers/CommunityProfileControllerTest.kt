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

class CommunityProfileControllerTest : AuthorizedControllerTest() {
  private lateinit var testData: CommunityStatsTestData

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  private val wednesday = LocalDate.of(2026, 3, 4)
  private val beyondHeatmap = wednesday.minusDays(400)

  @BeforeEach
  fun setup() {
    currentDateProvider.forcedDate = Date.from(wednesday.atTime(12, 0).toInstant(ZoneOffset.UTC))
    testData = CommunityStatsTestData()
    testDataService.saveTestData(testData.root)
    jdbcTemplate.update(
      "update community_contribution_day set day = ? where user_id = ? and language_id = ?",
      beyondHeatmap,
      testData.alice.id,
      testData.german.id,
    )
    userAccount = testData.bob
  }

  @AfterEach
  fun cleanup() {
    currentDateProvider.forcedDate = null
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `profile counts only public projects, newest first, heatmap limited to a year`() {
    performAuthGet("/v2/users/${testData.alice.id}/community-profile")
      .andIsOk
      .andAssertThatJson {
        node("id").isEqualTo(testData.alice.id)
        node("name").isEqualTo("Alice Adams")
        node("username").isAbsent()
        node("suggested").isEqualTo(4)
        node("accepted").isEqualTo(2)
        node("declined").isEqualTo(1)
        node("languages") {
          isArray.hasSize(2)
          node("[0].tag").isEqualTo("cs")
          node("[0].name").isEqualTo("Czech")
          node("[0].suggested").isEqualTo(3)
          node("[0].accepted").isEqualTo(2)
          node("[1].tag").isEqualTo("de")
          node("[1].declined").isEqualTo(1)
        }
        node("projects") {
          isArray.hasSize(1)
          node("[0].id").isEqualTo(testData.publicProject.id)
          node("[0].name").isEqualTo(CommunityStatsTestData.PUBLIC_PROJECT_NAME)
          node("[0].suggested").isEqualTo(4)
        }
        node("days") {
          isArray.hasSize(1)
          node("[0].day").isEqualTo("2026-03-04")
          node("[0].suggested").isEqualTo(3)
          node("[0].accepted").isEqualTo(2)
          node("[0].declined").isEqualTo(0)
        }
        node("recent") {
          isArray.hasSize(2)
          node("[0].projectId").isEqualTo(testData.publicProject.id)
          node("[0].languageTag").isEqualTo("cs")
          node("[0].day").isEqualTo("2026-03-04")
          node("[1].languageTag").isEqualTo("de")
          node("[1].day").isEqualTo(beyondHeatmap.toString())
        }
      }
  }

  @Test
  fun `a soft-deleted language drops out of the profile`() {
    jdbcTemplate.update("update language set deleted_at = now() where id = ?", testData.german.id)

    performAuthGet("/v2/users/${testData.alice.id}/community-profile")
      .andIsOk
      .andAssertThatJson {
        node("suggested").isEqualTo(3)
        node("declined").isEqualTo(0)
        node("languages").isArray.hasSize(1)
        node("recent").isArray.hasSize(1)
      }
  }

  @Test
  fun `unknown, disabled and never-publicly-contributing users are 404`() {
    performAuthGet("/v2/users/999999999/community-profile").andIsNotFound
    performAuthGet("/v2/users/${testData.dave.id}/community-profile").andIsNotFound
    performAuthGet("/v2/users/${testData.carol.id}/community-profile").andIsNotFound
  }
}
