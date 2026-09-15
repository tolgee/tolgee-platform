package io.tolgee.controllers.internal.e2eData

import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.controllers.internal.InternalController
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.SessionsTestData
import io.tolgee.model.enums.UserSessionType
import io.tolgee.security.authentication.JwtService
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import java.sql.Timestamp
import java.util.Date

@InternalController(["internal/e2e-data/sessions"])
class SessionsE2eDataController : AbstractE2eDataController() {
  @Autowired
  private lateinit var entityManager: EntityManager

  @Autowired
  private lateinit var currentDateProvider: CurrentDateProvider

  @Autowired
  private lateinit var jwtService: JwtService

  @Autowired
  private lateinit var authenticationProperties: AuthenticationProperties

  private var currentTestData: SessionsTestData? = null

  /**
   * A native login already yields a super-authenticated token, so nothing in the UI can prompt for
   * a password until that lapses. This hands the spec a token whose super window has already
   * closed, which is the only way to exercise the gate on the session listing.
   */
  @GetMapping("/expired-super-token")
  fun expiredSuperToken(): Map<String, String> {
    val user = currentTestData?.user ?: return emptyMap()
    val baseline = currentDateProvider.date
    currentDateProvider.forcedDate =
      Date(baseline.time - authenticationProperties.jwtSuperExpiration - 10_000)
    val token = jwtService.emitToken(user.id, type = UserSessionType.TEST, isSuper = true)
    currentDateProvider.forcedDate = null
    return mapOf("token" to token)
  }

  override val testData: TestDataBuilder
    get() {
      val data = SessionsTestData(currentDateProvider)
      currentTestData = data
      return data.root
    }

  override fun afterTestDataStored(data: TestDataBuilder) {
    val sessionsData = currentTestData ?: return

    sessionsData.createdAtByDeviceId.forEach { (deviceId, createdAt) ->
      entityManager
        .createNativeQuery("update user_session set created_at = :createdAt where device_id = :deviceId")
        .setParameter("createdAt", Timestamp(createdAt.time))
        .setParameter("deviceId", deviceId)
        .executeUpdate()
    }
  }
}
