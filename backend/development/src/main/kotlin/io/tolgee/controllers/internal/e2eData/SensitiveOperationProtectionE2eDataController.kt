package io.tolgee.controllers.internal.e2eData

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.AuthenticationProperties
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.SensitiveOperationProtectionTestData
import io.tolgee.security.authentication.JwtService
import io.tolgee.service.security.MfaService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/e2e-data/sensitive-operation-protection"])
@Transactional
class SensitiveOperationProtectionE2eDataController(
  private val authenticationProperties: AuthenticationProperties,
  private val currentDateProvider: CurrentDateProvider,
  private val mfaService: MfaService,
  private val jwtService: JwtService,
) : AbstractE2eDataController() {
  @GetMapping(value = ["/generate"])
  @Transactional
  fun generateBasicTestData(): Map<String, Any> {
    val data = SensitiveOperationProtectionTestData()
    testDataService.saveTestData(data.root)

    val baseline = currentDateProvider.date
    currentDateProvider.forcedDate = Date(baseline.time - authenticationProperties.jwtSuperExpiration - 10_000)
    val expiredToken = jwtService.emitToken(data.franta.id, true)
    currentDateProvider.forcedDate = null

    return mapOf(
      "frantasProjectId" to data.frantasProject.id,
      "pepasProjectId" to data.pepasProject.id,
      "frantaExpiredSuperJwt" to expiredToken,
      "pepaExpiredSuperJwt" to jwtService.emitToken(data.pepa.id, false),
    )
  }

  @GetMapping(value = ["/get-totp"])
  @Transactional
  fun getTotp(): Map<String, String> {
    return mapOf("otp" to mfaService.generateStringCode(SensitiveOperationProtectionTestData.TOTP_KEY))
  }

  override val testData: TestDataBuilder
    get() = SensitiveOperationProtectionTestData().root
}
