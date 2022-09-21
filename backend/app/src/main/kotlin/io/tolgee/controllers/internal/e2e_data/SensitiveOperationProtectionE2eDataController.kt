package io.tolgee.controllers.internal.e2e_data

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.development.testDataBuilder.TestDataService
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.data.SensitiveOperationProtectionTestData
import io.tolgee.security.InternalController
import io.tolgee.security.JwtTokenProvider
import io.tolgee.service.MfaService
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
@InternalController
class SensitiveOperationProtectionE2eDataController(
  private val testDataService: TestDataService,
  private val mfaService: MfaService,
  private val jwtTokenProvider: JwtTokenProvider
) : AbstractE2eDataController() {
  @GetMapping(value = ["/generate"])
  @Transactional
  fun generateBasicTestData(): Map<String, Any> {
    val data = SensitiveOperationProtectionTestData()
    testDataService.saveTestData(data.root)
    return mapOf(
      "frantasProjectId" to data.frantasProject.id,
      "pepasProjectId" to data.pepasProject.id,
      "frantaExpiredSuperJwt" to jwtTokenProvider.generateToken(data.franta.id, Date().time).toString(),
      "pepaExpiredSuperJwt" to jwtTokenProvider.generateToken(data.pepa.id, false).toString()
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
