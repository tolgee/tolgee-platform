package io.tolgee.controllers.internal.e2eData

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.NotificationsTestData
import io.tolgee.model.notifications.NotificationType
import io.tolgee.service.notification.NotificationService
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/e2e-data/notification"])
@Transactional
class NotificationE2eDataController : AbstractE2eDataController() {
  @Autowired
  private lateinit var notificationService: NotificationService

  @Autowired
  private lateinit var entityManager: EntityManager

  private lateinit var notificationsTestData: NotificationsTestData

  @PostMapping(value = ["/generate-notification"])
  @Transactional
  fun generateNotification(
    @RequestBody request: GenerateNotificationRequest,
  ) {
    val notification = notificationsTestData.generateNotificationWithTask()

    entityManager.persist(notification.linkedTask!!)

    notificationService.notify(
      notification.apply {
        this.type = request.type
      },
    )
  }

  override val testData: TestDataBuilder
    get() {
      notificationsTestData = NotificationsTestData()
      return notificationsTestData.root
    }
}

data class GenerateNotificationRequest(
  val userId: Long,
  val type: NotificationType,
)
