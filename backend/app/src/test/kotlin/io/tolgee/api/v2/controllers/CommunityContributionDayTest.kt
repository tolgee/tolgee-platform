package io.tolgee.api.v2.controllers

import io.tolgee.development.testDataBuilder.data.CommunityContributionDayTestData
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Date
import java.sql.Timestamp
import java.time.LocalDate

class CommunityContributionDayTest : AuthorizedControllerTest() {
  lateinit var testData: CommunityContributionDayTestData

  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  private val day1 = LocalDate.of(2026, 3, 2)
  private val day2 = LocalDate.of(2026, 3, 5)

  private data class Row(
    val day: LocalDate,
    val suggested: Int,
    val accepted: Int,
    val declined: Int,
  )

  @BeforeEach
  fun setup() {
    testData = CommunityContributionDayTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun clean() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `a full-row rewrite with an unchanged state counts nothing`() {
    val id = insertSuggestion(createdAt = day1)

    jdbcTemplate.update(
      "update translation_suggestion set translation = 'edited', state = state, updated_at = ? where id = ?",
      Timestamp.valueOf(day2.atStartOfDay()),
      id,
    )

    assertThat(rows()).containsExactly(Row(day1, suggested = 1, accepted = 0, declined = 0))
  }

  @Test
  fun `a state change counts on the day of the change`() {
    val id = insertSuggestion(createdAt = day1)

    jdbcTemplate.update(
      "update translation_suggestion set state = 'ACCEPTED', updated_at = ? where id = ?",
      Timestamp.valueOf(day2.atStartOfDay()),
      id,
    )

    assertThat(rows()).containsExactly(
      Row(day1, suggested = 1, accepted = 0, declined = 0),
      Row(day2, suggested = 0, accepted = 1, declined = 0),
    )
  }

  @Test
  fun `the backfill rebuilds what the trigger wrote`() {
    insertSuggestion(createdAt = day1)
    insertSuggestion(createdAt = day1, state = "DECLINED", updatedAt = day2)
    insertSuggestion(createdAt = day2, state = "ACCEPTED", updatedAt = day2)
    val fromTrigger = rows()

    jdbcTemplate.update("delete from community_contribution_day where project_id = ?", testData.project.id)
    runBackfill()

    assertThat(fromTrigger).containsExactly(
      Row(day1, suggested = 2, accepted = 0, declined = 0),
      Row(day2, suggested = 1, accepted = 1, declined = 1),
    )
    assertThat(rows()).isEqualTo(fromTrigger)
  }

  @Test
  fun `the backfill overwrites stale counts instead of adding to them`() {
    insertSuggestion(createdAt = day1)
    jdbcTemplate.update(
      "update community_contribution_day set suggested = 5 where project_id = ?",
      testData.project.id,
    )

    runBackfill()

    assertThat(rows()).containsExactly(Row(day1, suggested = 1, accepted = 0, declined = 0))
  }

  private fun runBackfill() {
    jdbcTemplate.execute(
      ClassPathResource("db/changelog/communityContributionDayBackfill.sql").getContentAsString(Charsets.UTF_8),
    )
  }

  private fun rows(): List<Row> =
    jdbcTemplate
      .queryForList(
        "select day, suggested, accepted, declined from community_contribution_day " +
          "where project_id = ? and user_id = ? and language_id = ? order by day",
        testData.project.id,
        testData.user.id,
        testData.englishLanguage.id,
      ).map {
        Row((it["day"] as Date).toLocalDate(), it["suggested"] as Int, it["accepted"] as Int, it["declined"] as Int)
      }

  private fun insertSuggestion(
    createdAt: LocalDate,
    state: String = "ACTIVE",
    updatedAt: LocalDate = createdAt,
  ): Long {
    val id = jdbcTemplate.queryForObject("select nextval('hibernate_sequence')", Long::class.java)!!
    jdbcTemplate.update(
      "insert into translation_suggestion " +
        "(id, project_id, language_id, key_id, author_id, translation, is_plural, state, created_at, updated_at) " +
        "values (?, ?, ?, ?, ?, 'text', false, ?, ?, ?)",
      id,
      testData.project.id,
      testData.englishLanguage.id,
      testData.key.id,
      testData.user.id,
      state,
      Timestamp.valueOf(createdAt.atStartOfDay()),
      Timestamp.valueOf(updatedAt.atStartOfDay()),
    )
    return id
  }
}
