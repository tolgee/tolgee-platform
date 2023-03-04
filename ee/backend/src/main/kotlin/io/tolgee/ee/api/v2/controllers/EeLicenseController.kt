package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.ee.api.v2.hateoas.EeUsageModel
import io.tolgee.ee.api.v2.hateoas.PrepareSetEeLicenceKeyModel
import io.tolgee.ee.data.SetLicenseKeyDto
import io.tolgee.ee.model.EeSubscription
import io.tolgee.ee.service.EeSubscriptionService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.security.UserAccountService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/ee-license/")
@Tag(name = "EE Licence (only for self-hosted instances)")
class EeLicenseController(
  private val securityService: SecurityService,
  private val eeSubscriptionService: EeSubscriptionService,
  private val userAccountService: UserAccountService
) {
  @PutMapping("set-license-key")
  @Operation(summary = "Sets the EE licence key for this instance")
  fun setLicenseKey(@RequestBody body: SetLicenseKeyDto): EeUsageModel {
    securityService.checkUserIsServerAdmin()
    val eeSubscription = eeSubscriptionService.setLicenceKey(body.licenseKey)
    return getEeUsageModel(eeSubscription)
  }

  @PostMapping("prepare-set-license-key")
  @Operation(summary = "Returns info about the upcoming EE subscription")
  fun prepareSetLicenseKey(@RequestBody body: SetLicenseKeyDto): PrepareSetEeLicenceKeyModel {
    securityService.checkUserIsServerAdmin()
    return eeSubscriptionService.prepareSetLicenceKey(body.licenseKey)
  }

  @GetMapping("info")
  @Operation(summary = "Returns the info about the current EE subscription")
  fun getInfo(): EeUsageModel? {
    val eeSubscription = eeSubscriptionService.getSubscription()
    return eeSubscription?.let { getEeUsageModel(it) }
  }

  fun getEeUsageModel(eeSubscription: EeSubscription): EeUsageModel {
    val currentUserCount = userAccountService.countAll()

    return EeUsageModel(
      enabledFeatures = eeSubscription.enabledFeatures,
      currentPeriodEnd = eeSubscription.currentPeriodEnd?.time,
      cancelAtPeriodEnd = eeSubscription.cancelAtPeriodEnd,
      currentUserCount = currentUserCount,
      userLimit = eeSubscription.userLimit
    )
  }
}
