package io.tolgee.development

import io.tolgee.development.testDataBuilder.data.ContributorsTestData
import io.tolgee.testing.AuthorizedControllerTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp

class ContributionTestDataSaverTest : AuthorizedControllerTest() {
  private lateinit var testData: ContributorsTestData

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @BeforeEach
  fun setup() {
    testData = ContributorsTestData(withE2eContributions = true)
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `contributions declared on the fixture reach project_contributor`() {
    val row =
      jdbcTemplate
        .queryForList(
          "select first_contribution_at, last_contribution_at from project_contributor " +
            "where project_id = ? and user_id = ?",
          testData.publicProject.id,
          testData.contributor.id,
        ).single()

    assertThat(row["first_contribution_at"])
      .isEqualTo(Timestamp(ContributorsTestData.FIRST_CONTRIBUTION_AT.time))
    assertThat(row["last_contribution_at"])
      .isEqualTo(Timestamp(ContributorsTestData.LAST_CONTRIBUTION_AT.time))

    assertThat(
      jdbcTemplate.queryForList(
        "select 1 from project_contributor where project_id = ? and user_id = ?",
        testData.project.id,
        testData.contributor.id,
      ),
    ).hasSize(1)
  }
}
