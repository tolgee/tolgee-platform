package io.tolgee.controllers.internal.e2eData

import io.tolgee.component.CurrentDateProvider
import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.SessionsTestData
import io.tolgee.model.EmailVerification
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import java.sql.Timestamp

@InternalController(["internal/e2e-data/sessions"])
class SessionsE2eDataController : AbstractE2eDataController() {
  @Autowired
  private lateinit var entityManager: EntityManager

  @Autowired
  private lateinit var currentDateProvider: CurrentDateProvider

  private var currentTestData: SessionsTestData? = null

  override val testData: TestDataBuilder
    get() {
      val data = SessionsTestData(currentDateProvider)
      currentTestData = data
      return data.root
    }

  override fun afterTestDataStored(data: TestDataBuilder) {
    val sessionsData = currentTestData ?: return

    entityManager.persist(
      EmailVerification(
        code = "sessions-e2e-verification-code",
        userAccount = sessionsData.unverifiedUser,
      ),
    )
    entityManager.flush()

    sessionsData.createdAtByDeviceId.forEach { (deviceId, createdAt) ->
      entityManager
        .createNativeQuery("update user_session set created_at = :createdAt where device_id = :deviceId")
        .setParameter("createdAt", Timestamp(createdAt.time))
        .setParameter("deviceId", deviceId)
        .executeUpdate()
    }
  }
}
