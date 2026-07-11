package io.tolgee.ee.api.v2.controllers.batch

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.batch.BatchJobChunkExecutionQueue
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class BatchJobTestBase {
  lateinit var testData: BatchJobsTestData

  @Autowired
  lateinit var batchJobOperationQueue: BatchJobChunkExecutionQueue

  @Autowired
  private lateinit var testDataService: TestDataService

  fun setup() {
    batchJobOperationQueue.clear()
    testData = BatchJobsTestData()
  }

  fun saveAndPrepare(testClass: ProjectAuthControllerTest) {
    testDataService.saveTestData(testData.root)
    testClass.userAccount = testData.user
    testClass.projectSupplier = { testData.projectBuilder.self }
  }
}
