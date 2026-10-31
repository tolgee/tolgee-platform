package io.tolgee.api.v2.controllers

import io.tolgee.activity.data.ActivityType
import io.tolgee.development.testDataBuilder.data.CommunityContributionDayTestData
import io.tolgee.dtos.request.translation.SetTranslationsWithKeyDto
import io.tolgee.dtos.request.translation.comment.TranslationCommentWithLangKeyDto
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.UserAccount
import io.tolgee.service.contributor.CommunityStatsService
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
    val userId: Long,
    val languageId: Long,
    val day: LocalDate,
    val contributions: Int,
    val suggested: Int,
    val accepted: Int,
    val declined: Int,
  )

  @BeforeEach
  fun setup() {
    testData = CommunityContributionDayTestData()
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
  }

  @AfterEach
  fun clean() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `an edit counts one contribution per language it touched`() {
    executeInNewTransaction {
      val rev = revision(ActivityType.SET_TRANSLATIONS)
      translation(rev, 1, testData.englishLanguage.id)
      translation(rev, 2, testData.englishLanguage.id)
      translation(rev, 3, testData.german.id)
    }
    assertThat(rows()).containsExactlyInAnyOrder(
      row(testData.user, testData.englishLanguage.id, day1, contributions = 1),
      row(testData.user, testData.german.id, day1, contributions = 1),
    )
  }

  @Test
  fun `suggesting counts as suggested and as a contribution of the suggester`() {
    executeInNewTransaction {
      val rev = revision(ActivityType.CREATE_SUGGESTION, author = testData.suggester)
      modified(rev, "TranslationSuggestion", testData.suggestion.id, keyAndLanguage(testData.englishLanguage.id))
    }
    assertThat(rows()).containsExactlyInAnyOrder(
      row(testData.suggester, testData.englishLanguage.id, day1, contributions = 1, suggested = 1),
    )
  }

  @Test
  fun `accepting with declineOther credits both suggesters and the moderator in one revision`() {
    executeInNewTransaction {
      val rev = revision(ActivityType.ACCEPT_SUGGESTION, at = day2)
      modified(
        rev,
        "TranslationSuggestion",
        testData.suggestion.id,
        keyAndLanguage(testData.englishLanguage.id),
        stateChange("ACCEPTED"),
      )
      modified(
        rev,
        "TranslationSuggestion",
        testData.otherSuggestion.id,
        keyAndLanguage(testData.englishLanguage.id),
        stateChange("DECLINED"),
      )
    }
    assertThat(rows()).containsExactlyInAnyOrder(
      row(testData.user, testData.englishLanguage.id, day2, contributions = 1),
      row(testData.suggester, testData.englishLanguage.id, day2, accepted = 1),
      row(testData.otherSuggester, testData.englishLanguage.id, day2, declined = 1),
    )
  }

  @Test
  fun `reactivating a suggestion is a contribution of the moderator and nothing else`() {
    executeInNewTransaction {
      val rev = revision(ActivityType.SUGGESTION_SET_ACTIVE)
      modified(
        rev,
        "TranslationSuggestion",
        testData.declinedSuggestion.id,
        keyAndLanguage(testData.englishLanguage.id),
        stateChange("ACTIVE", "DECLINED"),
      )
    }
    assertThat(
      rows(),
    ).containsExactlyInAnyOrder(row(testData.user, testData.englishLanguage.id, day1, contributions = 1))
  }

  @Test
  fun `types outside the allowlist and null-typed revisions count nothing`() {
    executeInNewTransaction {
      translation(revision(ActivityType.IMPORT), 1, testData.englishLanguage.id)
      translation(revision(ActivityType.BATCH_MACHINE_TRANSLATE), 2, testData.englishLanguage.id)
      translation(revision(null), 3, testData.englishLanguage.id)
    }
    assertThat(rows()).isEmpty()
  }

  @Test
  fun `a revision that touched no translation language counts nothing`() {
    executeInNewTransaction {
      modified(revision(ActivityType.COMPLEX_EDIT), "Key", testData.key.id, """{"namespace": null}""")
    }
    assertThat(rows()).isEmpty()
  }

  @Test
  fun `comment rows logged before comments described their language are stamped by the migration, then counted`() {
    executeInNewTransaction {
      val rev = revision(ActivityType.TRANSLATION_COMMENT_ADD)
      modified(rev, "TranslationComment", 1, """{"translation": ${ref("Translation", 7)}}""")
      describing(rev, "Translation", 7, keyAndLanguage(testData.german.id))
      describing(rev, "Translation", 8, keyAndLanguage(testData.englishLanguage.id))
    }
    assertThat(rows()).isEmpty()
    jdbcTemplate.execute(sql("translationCommentActivityLanguage.sql"))
    jdbcTemplate.execute(sql("communityContributionBackfill.sql"))
    assertThat(rows()).containsExactly(row(testData.user, testData.german.id, day1, contributions = 1))
  }

  @Test
  fun `the backfill rebuilds what the trigger wrote`() {
    executeInNewTransaction {
      translation(revision(ActivityType.SET_TRANSLATIONS), 1, testData.englishLanguage.id)
      translation(revision(ActivityType.SET_TRANSLATIONS), 2, testData.englishLanguage.id)
      modified(
        revision(ActivityType.TRANSLATION_COMMENT_ADD, author = testData.otherSuggester),
        "TranslationComment",
        1,
        """{"translation": ${ref("Translation", 7)}, "language": ${ref("Language", testData.german.id)}}""",
      )
      modified(
        revision(ActivityType.CREATE_SUGGESTION, author = testData.suggester),
        "TranslationSuggestion",
        testData.suggestion.id,
        keyAndLanguage(testData.german.id),
      )
      modified(
        revision(ActivityType.ACCEPT_SUGGESTION, at = day2),
        "TranslationSuggestion",
        testData.suggestion.id,
        keyAndLanguage(testData.german.id),
        stateChange("ACCEPTED"),
      )
      translation(revision(ActivityType.IMPORT), 3, testData.englishLanguage.id)
    }
    val fromTrigger = rows()
    assertThat(fromTrigger).containsExactlyInAnyOrder(
      row(testData.user, testData.englishLanguage.id, day1, contributions = 2),
      row(testData.user, testData.german.id, day2, contributions = 1),
      row(testData.otherSuggester, testData.german.id, day1, contributions = 1),
      row(testData.suggester, testData.german.id, day1, contributions = 1, suggested = 1),
      row(testData.suggester, testData.german.id, day2, accepted = 1),
    )
    jdbcTemplate.update(
      "update community_contribution_day set contributions = 9 where project_id = ?",
      testData.project.id,
    )
    jdbcTemplate.execute(sql("communityContributionBackfill.sql"))
    assertThat(rows()).isEqualTo(fromTrigger)
  }

  @Test
  fun `the SQL allowlist and the Kotlin one agree`() {
    ActivityType.entries.forEach { type ->
      val sql = jdbcTemplate.queryForObject("select is_translation_contribution(?)", Boolean::class.java, type.name)
      assertThat(sql).describedAs(type.name).isEqualTo(type in CommunityStatsService.CONTRIBUTION_TYPES)
    }
    assertThat(jdbcTemplate.queryForObject("select is_translation_contribution(null)", Boolean::class.java)).isFalse
  }

  @Test
  fun `an edit and a comment made through the API are counted`() {
    performAuthPut(
      "/v2/projects/${testData.project.id}/translations",
      SetTranslationsWithKeyDto(key = testData.key.name, translations = mapOf("en" to "Hello")),
    ).andIsOk
    performAuthPost(
      "/v2/projects/${testData.project.id}/translations/create-comment",
      TranslationCommentWithLangKeyDto(
        keyId = testData.key.id,
        languageId = testData.englishLanguage.id,
        text = "Nice",
      ),
    ).andIsCreated
    assertThat(rows()).containsExactlyInAnyOrder(
      row(testData.user, testData.englishLanguage.id, currentDateProvider.localDate, contributions = 2),
    )
  }

  private fun revision(
    type: ActivityType?,
    author: UserAccount = testData.user,
    at: LocalDate = day1,
  ): Long {
    val id = jdbcTemplate.queryForObject("select nextval('activity_sequence')", Long::class.java)!!
    jdbcTemplate.update(
      "insert into activity_revision (id, project_id, author_id, type, \"timestamp\") values (?, ?, ?, ?, ?)",
      id,
      testData.project.id,
      author.id,
      type?.name,
      Timestamp.valueOf(at.atStartOfDay()),
    )
    return id
  }

  private fun translation(
    rev: Long,
    entityId: Long,
    languageId: Long,
  ) = modified(rev, "Translation", entityId, keyAndLanguage(languageId))

  private fun modified(
    rev: Long,
    entityClass: String,
    entityId: Long,
    describingRelations: String,
    modifications: String = """{"text": {"old": null, "new": "x"}}""",
  ) {
    jdbcTemplate.update(
      "insert into activity_modified_entity " +
        "(activity_revision_id, entity_class, entity_id, revision_type, modifications, describing_relations) " +
        "values (?, ?, ?, 1, ?::jsonb, ?::jsonb)",
      rev,
      entityClass,
      entityId,
      modifications,
      describingRelations,
    )
  }

  private fun sql(file: String) = ClassPathResource("db/changelog/$file").getContentAsString(Charsets.UTF_8)

  private fun describing(
    rev: Long,
    entityClass: String,
    entityId: Long,
    describingRelations: String,
  ) {
    jdbcTemplate.update(
      "insert into activity_describing_entity " +
        "(activity_revision_id, entity_class, entity_id, data, describing_relations) " +
        "values (?, ?, ?, '{}'::jsonb, ?::jsonb)",
      rev,
      entityClass,
      entityId,
      describingRelations,
    )
  }

  private fun ref(
    entityClass: String,
    entityId: Long,
  ) = """{"entityClass": "$entityClass", "entityId": $entityId}"""

  private fun keyAndLanguage(languageId: Long) =
    """{"key": ${ref("Key", testData.key.id)}, "language": ${ref("Language", languageId)}}"""

  private fun stateChange(
    new: String,
    old: String = "ACTIVE",
  ) = """{"state": {"old": "$old", "new": "$new"}}"""

  private fun row(
    user: UserAccount,
    languageId: Long,
    day: LocalDate,
    contributions: Int = 0,
    suggested: Int = 0,
    accepted: Int = 0,
    declined: Int = 0,
  ) = Row(user.id, languageId, day, contributions, suggested, accepted, declined)

  private fun rows(): List<Row> =
    jdbcTemplate
      .queryForList(
        "select user_id, language_id, day, contributions, suggested, accepted, declined " +
          "from community_contribution_day " +
          "where project_id = ? order by user_id, language_id, day",
        testData.project.id,
      ).map {
        Row(
          it["user_id"] as Long,
          it["language_id"] as Long,
          (it["day"] as Date).toLocalDate(),
          it["contributions"] as Int,
          it["suggested"] as Int,
          it["accepted"] as Int,
          it["declined"] as Int,
        )
      }
}
