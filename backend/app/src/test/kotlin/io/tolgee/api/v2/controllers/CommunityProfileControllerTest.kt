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
    executeInNewTransaction { testData.replayActivity(entityManager) }
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
  fun `profile counts only public projects, pending included, heatmap limited to a year`() {
    performAuthGet("/v2/users/${testData.alice.id}/community-profile")
      .andIsOk
      .andAssertThatJson {
        node("id").isEqualTo(testData.alice.id)
        node("name").isEqualTo("Alice Adams")
        node("username").isAbsent()
        node("recent").isAbsent()
        node("contributions").isEqualTo(5)
        node("suggested").isEqualTo(4)
        node("accepted").isEqualTo(2)
        node("declined").isEqualTo(1)
        node("active").isEqualTo(1)
        node("languages") {
          isArray.hasSize(2)
          node("[0].tag").isEqualTo("cs")
          node("[0].name").isEqualTo("Czech")
          node("[0].contributions").isEqualTo(4)
          node("[0].suggested").isEqualTo(3)
          node("[0].accepted").isEqualTo(2)
          node("[1].tag").isEqualTo("de")
          node("[1].declined").isEqualTo(1)
        }
        node("projects") {
          isArray.hasSize(1)
          node("[0].id").isEqualTo(testData.publicProject.id)
          node("[0].name").isEqualTo(CommunityStatsTestData.PUBLIC_PROJECT_NAME)
          node("[0].contributions").isEqualTo(5)
          node("[0].suggested").isEqualTo(4)
          node("[0].active").isEqualTo(1)
          node("[0].firstContributionAt").isEqualTo("2026-03-04T12:00:00.000Z")
          node("[0].lastContributionAt").isEqualTo("2026-03-04T12:00:00.000Z")
        }
        node("days").isAbsent()
        node("buckets") {
          isArray.hasSize(365)
          node("[0].start").isEqualTo("2025-03-05")
          node("[0].contributions").isEqualTo(0)
          node("[364].start").isEqualTo("2026-03-04")
          node("[364].contributions").isEqualTo(4)
          node("[364].suggested").isEqualTo(3)
          node("[364].accepted").isEqualTo(2)
          node("[364].declined").isEqualTo(0)
        }
      }
  }

  @Test
  fun `profile buckets take the range and granularity and keep days outside the range out, totals stay all-time`() {
    performAuthGet("/v2/users/${testData.alice.id}/community-profile?from=2026-03-04&to=2026-03-04&granularity=WEEK")
      .andIsOk
      .andAssertThatJson {
        node("contributions").isEqualTo(5)
        node("buckets") {
          isArray.hasSize(1)
          node("[0].start").isEqualTo("2026-03-02")
          node("[0].contributions").isEqualTo(4)
        }
      }
    performAuthGet("/v2/users/${testData.alice.id}/community-profile?from=2026-03-02&to=2026-03-03&granularity=WEEK")
      .andIsOk
      .andAssertThatJson {
        node("buckets") {
          isArray.hasSize(1)
          node("[0].start").isEqualTo("2026-03-02")
          node("[0].contributions").isEqualTo(0)
        }
      }
  }

  @Test
  fun `a soft-deleted language drops out of the profile`() {
    jdbcTemplate.update("update language set deleted_at = now() where id = ?", testData.german.id)
    jdbcTemplate.update(
      "update translation_suggestion set state = 'ACTIVE' where language_id = ?",
      testData.german.id,
    )
    performAuthGet("/v2/users/${testData.alice.id}/community-profile")
      .andIsOk
      .andAssertThatJson {
        node("contributions").isEqualTo(4)
        node("suggested").isEqualTo(3)
        node("declined").isEqualTo(0)
        node("active").isEqualTo(1)
        node("languages").isArray.hasSize(1)
      }
  }

  @Test
  fun `unknown, disabled and never-publicly-contributing users are 404`() {
    performAuthGet("/v2/users/999999999/community-profile").andIsNotFound
    performAuthGet("/v2/users/${testData.dave.id}/community-profile").andIsNotFound
    performAuthGet("/v2/users/${testData.carol.id}/community-profile").andIsNotFound
  }

  @Test
  fun `the feed lists public translation activity newest first with project, key and language, no author`() {
    jdbcTemplate.update(
      "insert into activity_revision (id, project_id, author_id, type, \"timestamp\") " +
        "values (nextval('activity_sequence'), ?, ?, 'CREATE_KEY', now())",
      testData.publicProject.id,
      testData.alice.id,
    )
    performAuthGet("/v2/users/${testData.alice.id}/community-activity")
      .andIsOk
      .andAssertThatJson {
        node("page.totalElements").isEqualTo(5)
        node("_embedded.activities") {
          isArray.hasSize(5)
          node("[0].type").isEqualTo("TRANSLATION_COMMENT_ADD")
          node("[0].timestamp").isEqualTo(currentDateProvider.date.time)
          node("[0].project.id").isEqualTo(testData.publicProject.id)
          node("[0].project.name").isEqualTo(CommunityStatsTestData.PUBLIC_PROJECT_NAME)
          node("[0].author").isAbsent()
          node("[0].modifiedEntities.TranslationComment[0].modifications.text.new").isEqualTo("Looks right")
          node("[0].modifiedEntities.TranslationComment[0].relations.translation.relations.key.data.name")
            .isEqualTo("key-1")
          node("[0].modifiedEntities.TranslationComment[0].relations.translation.relations.language.data.tag")
            .isEqualTo("cs")
          node("[1].type").isEqualTo("CREATE_SUGGESTION")
          node("[1].modifiedEntities.TranslationSuggestion[0].relations.key.data.name").isEqualTo("key-1")
          node("[1].modifiedEntities.TranslationSuggestion[0].relations.language.data.tag").isEqualTo("de")
          node("[4].type").isEqualTo("CREATE_SUGGESTION")
        }
      }
    performAuthGet("/v2/users/${testData.alice.id}/community-activity?size=2&page=1")
      .andIsOk
      .andAssertThatJson {
        node("_embedded.activities").isArray.hasSize(2)
        node("page.totalElements").isEqualTo(5)
      }
  }

  @Test
  fun `the feed is hidden exactly where the profile is`() {
    performAuthGet("/v2/users/999999999/community-activity").andIsNotFound
    performAuthGet("/v2/users/${testData.dave.id}/community-activity").andIsNotFound
    performAuthGet("/v2/users/${testData.carol.id}/community-activity").andIsNotFound
  }
}
