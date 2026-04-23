package io.tolgee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobChunkExecutionQueue
import io.tolgee.batch.BatchJobService
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.configuration.tolgee.machineTranslation.MachineTranslationProperties
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.waitFor
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.model.translation.Translation
import io.tolgee.testing.assert
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.test.web.servlet.ResultActions
import java.util.function.Consumer

@Component
class BatchJobTestBase {
  lateinit var testData: BatchJobsTestData

  @Autowired
  lateinit var batchJobOperationQueue: BatchJobChunkExecutionQueue

  @Autowired
  lateinit var batchJobService: BatchJobService

  @Autowired
  lateinit var tolgeeProperties: TolgeeProperties

  val machineTranslationProperties: MachineTranslationProperties get() = tolgeeProperties.machineTranslation

  @Autowired
  lateinit var entityManager: EntityManager

  var fakeBefore: Boolean = false

  // Snapshots of TolgeeProperties fields mutated in setup(). The properties bean is a
  // singleton in the Spring context, and with the context shared across tests these mutations
  // would leak between test classes. tearDown() restores them to the captured values.
  private var fakeMtProvidersBefore: Boolean = false
  private var googleApiKeyBefore: String? = null
  private var googleDefaultEnabledBefore: Boolean = false
  private var googleDefaultPrimaryBefore: Boolean = false
  private var awsDefaultEnabledBefore: Boolean = false
  private var awsAccessKeyBefore: String? = null
  private var awsSecretKeyBefore: String? = null

  @Autowired
  private lateinit var testDataService: TestDataService

  fun setup() {
    batchJobOperationQueue.clear()
    testData = BatchJobsTestData()

    // Snapshot current values before mutating, so tearDown() can restore them
    fakeMtProvidersBefore = tolgeeProperties.internal.fakeMtProviders
    googleApiKeyBefore = machineTranslationProperties.google.apiKey
    googleDefaultEnabledBefore = machineTranslationProperties.google.defaultEnabled
    googleDefaultPrimaryBefore = machineTranslationProperties.google.defaultPrimary
    awsDefaultEnabledBefore = machineTranslationProperties.aws.defaultEnabled
    awsAccessKeyBefore = machineTranslationProperties.aws.accessKey
    awsSecretKeyBefore = machineTranslationProperties.aws.secretKey

    // Set properties directly instead of mocking
    tolgeeProperties.internal.fakeMtProviders = true

    // Configure Google MT
    machineTranslationProperties.google.apiKey = "mock"
    machineTranslationProperties.google.defaultEnabled = true
    machineTranslationProperties.google.defaultPrimary = true

    // Configure AWS MT
    machineTranslationProperties.aws.defaultEnabled = false
    machineTranslationProperties.aws.accessKey = "mock"
    machineTranslationProperties.aws.secretKey = "mock"
  }

  fun tearDown() {
    // Restore shared TolgeeProperties singleton to avoid cross-test leakage
    tolgeeProperties.internal.fakeMtProviders = fakeMtProvidersBefore
    machineTranslationProperties.google.apiKey = googleApiKeyBefore
    machineTranslationProperties.google.defaultEnabled = googleDefaultEnabledBefore
    machineTranslationProperties.google.defaultPrimary = googleDefaultPrimaryBefore
    machineTranslationProperties.aws.defaultEnabled = awsDefaultEnabledBefore
    machineTranslationProperties.aws.accessKey = awsAccessKeyBefore
    machineTranslationProperties.aws.secretKey = awsSecretKeyBefore
  }

  fun saveAndPrepare(testClass: ProjectAuthControllerTest) {
    testDataService.saveTestData(testData.root)
    testClass.userAccount = testData.user
    testClass.projectSupplier = { testData.projectBuilder.self }
  }

  fun waitForAllTranslated(
    keyIds: List<Long>,
    keyCount: Int,
    expectedCsValue: String = "translated with GOOGLE from en to cs",
  ) {
    waitForNotThrowing(pollTime = 1000, timeout = 60000) {
      @Suppress("UNCHECKED_CAST")
      val czechTranslations =
        entityManager
          .createQuery(
            """
            from Translation t where t.key.id in :keyIds and t.language.tag = 'cs'
            """.trimIndent(),
          ).setParameter("keyIds", keyIds)
          .resultList as List<Translation>
      czechTranslations.assert.hasSize(keyCount)
      czechTranslations.forEach {
        it.text.assert.contains(expectedCsValue)
      }
    }
  }

  fun waitForJobCompleted(resultActions: ResultActions) =
    resultActions.andAssertThatJson {
      this.node("id").isNumber.satisfies(
        Consumer {
          waitFor(pollTime = 2000) {
            val job = batchJobService.findJobDto(it.toLong())
            job?.status?.completed == true
          }
        },
      )
    }
}
