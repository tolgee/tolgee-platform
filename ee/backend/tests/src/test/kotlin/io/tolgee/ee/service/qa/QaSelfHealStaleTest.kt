package io.tolgee.ee.service.qa

import io.tolgee.AbstractSpringTest
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.QaCheckRequest
import io.tolgee.ee.development.QaTestData
import io.tolgee.model.Project
import io.tolgee.model.translation.Translation
import io.tolgee.testing.assert
import io.tolgee.util.executeInNewTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class QaSelfHealStaleTest : AbstractSpringTest() {
  @Autowired
  private lateinit var qaRecheckService: QaRecheckService

  @Autowired
  private lateinit var batchJobService: BatchJobService

  private lateinit var testData: QaTestData

  @BeforeEach
  fun setup() {
    testData = QaTestData()
    testDataService.saveTestData(testData.root)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `enqueues self-heal QA job when stale translations exist`() {
    val baseline = qaJobCount()

    qaRecheckService.recheckStuckStaleTranslationsInProject(testData.project.id)

    qaJobCount().assert.isEqualTo(baseline + 1)
  }

  @Test
  fun `does not enqueue when no stale translations`() {
    executeInNewTransaction(platformTransactionManager) {
      // FIXME: This should be a function in test data to optionally modify it's state before we save the test data
      entityManager
        .createQuery(
          "update Translation t set t.qaChecksStale = false " +
            "where t.key.project.id = :pid",
        ).setParameter("pid", testData.project.id)
        .executeUpdate()
    }
    val baseline = qaJobCount()

    qaRecheckService.recheckStuckStaleTranslationsInProject(testData.project.id)

    qaJobCount().assert.isEqualTo(baseline)
  }

  @Test
  fun `does not enqueue when another QA job is already active`() {
    val staleTranslation =
      executeInNewTransaction(platformTransactionManager) {
        // FIXME: This should be exposed by test data as it's propety - no need to query for stuff created by test data
        // when it isn't required by the test itself
        entityManager
          .createQuery(
            "select t from Translation t " +
              "where t.key.project.id = :pid and t.qaChecksStale = true",
            Translation::class.java,
          ).setParameter("pid", testData.project.id)
          .resultList
          .first()
      }
    // Manually enqueue a QA job so `hasActiveJobForProject` returns true.
    executeInNewTransaction(platformTransactionManager) {
      val projectRef = entityManager.getReference(Project::class.java, testData.project.id)
      batchJobService.startJob(
        request =
          QaCheckRequest(
            target =
              listOf(
                BatchTranslationTargetItem(
                  keyId = staleTranslation.key.id,
                  languageId = staleTranslation.language.id,
                ),
              ),
          ),
        project = projectRef,
        author = null,
        type = BatchJobType.QA_CHECK,
        isHidden = true,
      )
    }
    val baseline = qaJobCount()

    qaRecheckService.recheckStuckStaleTranslationsInProject(testData.project.id)

    qaJobCount().assert.isEqualTo(baseline)
  }

  @Test
  fun `does not enqueue when QA checks are disabled on project`() {
    executeInNewTransaction(platformTransactionManager) {
      // FIXME: This should be a function in test data to optionally modify it's state before we save the test data
      val project = entityManager.find(Project::class.java, testData.project.id)
      project.useQaChecks = false
    }
    val baseline = qaJobCount()

    qaRecheckService.recheckStuckStaleTranslationsInProject(testData.project.id)

    qaJobCount().assert.isEqualTo(baseline)
  }

  private fun qaJobCount(): Int =
    executeInNewTransaction(platformTransactionManager) {
      batchJobService.getAllByProjectId(testData.project.id).count { it.type == BatchJobType.QA_CHECK }
    }
}
