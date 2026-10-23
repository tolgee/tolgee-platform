package io.tolgee.ee.api.v2.controllers

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.development.testDataBuilder.data.SuggestionsTestData
import io.tolgee.ee.data.translationSuggestion.CreateTranslationSuggestionRequest
import io.tolgee.fixtures.andIsOk
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.SuggestionsMode
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

class CommunityContributionActivityTest : ProjectAuthControllerTest("/v2/projects/") {
  private lateinit var testData: SuggestionsTestData

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @BeforeEach
  fun setup() {
    testData = SuggestionsTestData(SuggestionsMode.ENABLED)
    projectSupplier = { testData.relatedProject.self }
    testDataService.saveTestData(testData.root)
    projectService.setPublic(testData.relatedProject.self.id, true)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `suggesting, accepting and declining through the API feed the counters`() {
    val key = testData.keys[0].self
    val czech = testData.czechLanguage
    userAccount = testData.communityUser.self
    performProjectAuthPost(
      "languages/${czech.id}/key/${key.id}/suggestion",
      CreateTranslationSuggestionRequest(translation = "Community suggestion"),
    ).andIsOk
    val suggestionId =
      jdbcTemplate.queryForObject(
        "select id from translation_suggestion where key_id = ? and author_id = ?",
        Long::class.java,
        key.id,
        testData.communityUser.self.id,
      )
    assertThat(czechCounts(testData.communityUser.self)).isEqualTo(Counts(contributions = 1, suggested = 1))
    assertThat(czechCounts(testData.projectReviewer.self)).isNull()

    userAccount = testData.projectReviewer.self
    performProjectAuthPut(
      "languages/${czech.id}/key/${key.id}/suggestion/$suggestionId/accept?declineOther=true",
    ).andIsOk
    assertThat(
      czechCounts(testData.communityUser.self),
    ).isEqualTo(Counts(contributions = 1, suggested = 1, accepted = 1))
    assertThat(czechCounts(testData.projectTranslator.self)).isEqualTo(Counts(declined = 1))
    assertThat(czechCounts(testData.projectReviewer.self))
      .describedAs("the reviewer's own suggestion was declined too")
      .isEqualTo(Counts(contributions = 1, declined = 1))
    assertThat(
      jdbcTemplate.queryForList(
        "select 1 from project_contributor where project_id = ? and user_id = ?",
        testData.relatedProject.self.id,
        testData.communityUser.self.id,
      ),
    ).hasSize(1)
  }

  private data class Counts(
    val contributions: Int = 0,
    val suggested: Int = 0,
    val accepted: Int = 0,
    val declined: Int = 0,
  )

  private fun czechCounts(user: UserAccount): Counts? =
    jdbcTemplate
      .queryForList(
        "select sum(contributions) c, sum(suggested) s, sum(accepted) a, sum(declined) d " +
          "from community_contribution_day where project_id = ? and user_id = ? and language_id = ? " +
          "having count(*) > 0",
        testData.relatedProject.self.id,
        user.id,
        testData.czechLanguage.id,
      ).singleOrNull()
      ?.let {
        Counts(
          (it["c"] as Number).toInt(),
          (it["s"] as Number).toInt(),
          (it["a"] as Number).toInt(),
          (it["d"] as Number).toInt(),
        )
      }
}
