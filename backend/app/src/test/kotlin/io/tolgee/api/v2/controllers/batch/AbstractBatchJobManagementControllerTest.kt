package io.tolgee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobActivityFinalizer
import io.tolgee.batch.BatchJobChunkExecutionQueue
import io.tolgee.batch.BatchJobConcurrentLauncher
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.BatchJobTestUtil
import io.tolgee.batch.processors.MachineTranslationChunkProcessor
import io.tolgee.batch.processors.PreTranslationByTmChunkProcessor
import io.tolgee.batch.state.BatchJobStateProvider
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import io.tolgee.service.machineTranslation.mtCreditsConsumption.MtCreditBucketService
import io.tolgee.service.translation.AutoTranslationService
import io.tolgee.util.BatchDumper
import io.tolgee.util.Logging
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

abstract class AbstractBatchJobManagementControllerTest(
  basePath: String,
) : ProjectAuthControllerTest(basePath),
  Logging {
  lateinit var testData: BatchJobsTestData

  @Autowired
  lateinit var batchJobService: BatchJobService

  @Autowired
  lateinit var batchJobStateProvider: BatchJobStateProvider

  @Autowired
  lateinit var batchJobChunkExecutionQueue: BatchJobChunkExecutionQueue

  @Autowired
  lateinit var batchJobConcurrentLauncher: BatchJobConcurrentLauncher

  @Suppress("LateinitVarOverridesLateinitVar")
  @Autowired
  @MockitoSpyBean
  override lateinit var mtCreditBucketService: MtCreditBucketService

  @Autowired
  @MockitoSpyBean
  lateinit var preTranslationByTmChunkProcessor: PreTranslationByTmChunkProcessor

  @Autowired
  @MockitoSpyBean
  lateinit var machineTranslationChunkProcessor: MachineTranslationChunkProcessor

  @Autowired
  @MockitoSpyBean
  lateinit var autoTranslationService: AutoTranslationService

  @MockitoSpyBean
  @Autowired
  lateinit var batchJobActivityFinalizer: BatchJobActivityFinalizer

  @Autowired
  lateinit var batchDumper: BatchDumper

  lateinit var util: BatchJobTestUtil

  @BeforeEach
  fun abstractSetup() {
    batchJobChunkExecutionQueue.clear()
    testData = BatchJobsTestData()
    batchJobChunkExecutionQueue.populateQueue()
    Mockito.reset(
      mtCreditBucketService,
      autoTranslationService,
      machineTranslationChunkProcessor,
      preTranslationByTmChunkProcessor,
      batchJobActivityFinalizer,
    )
    util = BatchJobTestUtil(applicationContext, testData)
  }

  protected fun saveAndPrepare() {
    testDataService.saveTestData(testData.root)
    userAccount = testData.user
    this.projectSupplier = { testData.projectBuilder.self }
  }
}
