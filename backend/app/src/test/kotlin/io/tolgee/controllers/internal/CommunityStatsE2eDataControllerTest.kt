package io.tolgee.controllers.internal

import io.tolgee.controllers.internal.e2eData.CommunityStatsE2eDataController
import io.tolgee.development.testDataBuilder.data.CommunityStatsTestData
import io.tolgee.testing.AbstractControllerTest
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate

@ContextRecreatingTest
@SpringBootTest(properties = ["tolgee.internal.controller-enabled=true"])
class CommunityStatsE2eDataControllerTest : AbstractControllerTest() {
  @Autowired
  lateinit var controller: CommunityStatsE2eDataController

  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  @AfterEach
  fun clean() {
    controller.cleanup()
  }

  @Test
  fun `the seed spreads contributions over several weeks for several contributors`() {
    val result = controller.generate()
    val projectId = result.projects.single { it.name == CommunityStatsTestData.PUBLIC_PROJECT_NAME }.id

    val row =
      jdbcTemplate
        .queryForMap(
          """
          select count(distinct date_trunc('week', day)) as weeks, count(distinct user_id) as users,
            sum(suggested) as suggested, sum(accepted) as accepted, sum(declined) as declined
          from community_contribution_day where project_id = ?
          """,
          projectId,
        )

    assertThat((row["weeks"] as Number).toInt()).isGreaterThanOrEqualTo(6)
    assertThat((row["users"] as Number).toInt()).isGreaterThanOrEqualTo(5)
    assertThat((row["suggested"] as Number).toInt()).isGreaterThan(40)
    assertThat((row["accepted"] as Number).toInt()).isGreaterThan(0)
    assertThat((row["declined"] as Number).toInt()).isGreaterThan(0)
  }
}
