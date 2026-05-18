package io.tolgee.ee.service.qa

import io.tolgee.AbstractSpringTest
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.QaCheckRequest
import io.tolgee.ee.development.QaTestData
import io.tolgee.model.Project
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
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `enqueues self-heal QA job when stale translations exist`() {
    testDataService.saveTestData(testData.root)
    val baseline = qaJobCount()

    qaRecheckService.recheckStuckStaleTranslationsInProject(testData.project.id)

    qaJobCount().assert.isEqualTo(baseline + 1)
  }

  @Test
  fun `does not enqueue when no stale translations`() {
    testData.clearAllStaleFlags()
    testDataService.saveTestData(testData.root)
    val baseline = qaJobCount()

    qaRecheckService.recheckStuckStaleTranslationsInProject(testData.project.id)

    qaJobCount().assert.isEqualTo(baseline)
  }

  @Test
  fun `does not enqueue when another QA job is already active`() {
    testDataService.saveTestData(testData.root)
    val staleTranslation = testData.staleFrTranslation
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
    testData.disableQaChecks()
    testDataService.saveTestData(testData.root)
    val baseline = qaJobCount()

    qaRecheckService.recheckStuckStaleTranslationsInProject(testData.project.id)

    qaJobCount().assert.isEqualTo(baseline)
  }

  private fun qaJobCount(): Int =
    executeInNewTransaction(platformTransactionManager) {
      batchJobService.getAllByProjectId(testData.project.id).count { it.type == BatchJobType.QA_CHECK }
    }
}
