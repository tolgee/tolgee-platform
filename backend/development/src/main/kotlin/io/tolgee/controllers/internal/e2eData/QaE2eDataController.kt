package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.QaE2eTestData
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired

@InternalController(["internal/e2e-data/qa"])
class QaE2eDataController : AbstractE2eDataController() {
  @Autowired
  private lateinit var entityManager: EntityManager

  private var currentTestData: QaE2eTestData? = null

  override val testData: TestDataBuilder
    get() {
      currentTestData = QaE2eTestData()
      return currentTestData!!.root
    }

  override fun afterTestDataStored(data: TestDataBuilder) {
    currentTestData?.let { entityManager.merge(it.qaConfig) }
  }
}
