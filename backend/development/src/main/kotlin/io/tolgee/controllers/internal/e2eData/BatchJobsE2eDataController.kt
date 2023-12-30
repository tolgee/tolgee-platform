package io.tolgee.controllers.internal.e2eData

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.BatchJobsTestData
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/e2e-data/batch-jobs"])
@Transactional
class BatchJobsE2eDataController : AbstractE2eDataController() {
  override val testData: TestDataBuilder
    get() {
      val data = BatchJobsTestData()
      data.addTranslationOperationData()
      return data.root
    }
}
