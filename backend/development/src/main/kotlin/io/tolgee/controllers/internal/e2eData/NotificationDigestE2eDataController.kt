package io.tolgee.controllers.internal.e2eData

import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.NotificationDigestE2eTestData
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired

@InternalController(["internal/e2e-data/notification-digest"])
class NotificationDigestE2eDataController : AbstractE2eDataController() {
  @Autowired
  private lateinit var entityManager: EntityManager

  private var currentTestData: NotificationDigestE2eTestData? = null

  override val testData: TestDataBuilder
    get() {
      currentTestData = NotificationDigestE2eTestData()
      return currentTestData!!.root
    }

  override fun afterTestDataStored(data: TestDataBuilder) {
    val testData = currentTestData ?: return
    entityManager
      .createNativeQuery("insert into notification_entity (notification_id, entity_id) values (:notification, :entity)")
      .setParameter("notification", testData.translatedNotification.id)
      .setParameter("entity", testData.frenchTranslation.id)
      .executeUpdate()
  }
}
