package io.tolgee.batch

import io.tolgee.AbstractSpringTest
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.DeleteKeysRequest
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.key.Key
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BatchJobContributionTest : AbstractSpringTest() {
  private lateinit var testData: BatchJobsTestData
  private lateinit var keys: List<Key>

  @Autowired
  lateinit var batchJobService: BatchJobService

  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  @BeforeEach
  fun setup() {
    testData = BatchJobsTestData()
    keys = testData.addTranslationOperationData(10)
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `records the job author as a contributor of the project the job ran on`() {
    val projectId = testData.projectBuilder.self.id
    val authorId = testData.user.id

    executeInNewTransaction(platformTransactionManager) {
      batchJobService.startJob(
        request = DeleteKeysRequest().apply { keyIds = keys.map { it.id } },
        project = testData.projectBuilder.self,
        author = testData.user,
        type = BatchJobType.DELETE_KEYS,
      )
    }

    waitForNotThrowing(pollTime = 200, timeout = 60_000) {
      jdbcTemplate
        .queryForList(
          "select 1 from project_contributor" +
            " where project_id = $projectId and user_id = $authorId",
        ).assert
        .hasSize(1)
    }
  }
}
