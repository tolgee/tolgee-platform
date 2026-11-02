package io.tolgee.controllers.internal.e2eData

import io.tolgee.activity.data.ActivityType
import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.AiReviewE2eTestData
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.enums.SystemActor
import io.tolgee.util.executeInNewTransaction
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager

@InternalController(["internal/e2e-data/ai-review"])
class AiReviewE2eDataController : AbstractE2eDataController() {
  @Autowired
  private lateinit var entityManager: EntityManager

  @Autowired
  private lateinit var transactionManager: PlatformTransactionManager

  private var currentTestData: AiReviewE2eTestData? = null

  override val testData: TestDataBuilder
    get() {
      currentTestData = AiReviewE2eTestData()
      return currentTestData!!.root
    }

  override fun afterTestDataStored(data: TestDataBuilder) {
    val testData = currentTestData ?: return
    executeInNewTransaction(transactionManager) {
      val revision = ActivityRevision()
      revision.type = ActivityType.SET_TRANSLATIONS
      revision.projectId = testData.project.id
      revision.organizationId = testData.project.organizationOwner.id
      revision.systemActor = SystemActor.AI_REVIEWER
      entityManager.persist(revision)
      entityManager.persist(
        ActivityModifiedEntity(
          activityRevision = revision,
          entityClass = "Translation",
          entityId = testData.approvedTranslation.self.id,
        ),
      )
    }
  }
}
