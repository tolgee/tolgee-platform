package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.ee.api.v2.hateoas.PrepareSetEeLicenceKeyModel
import io.tolgee.ee.api.v2.hateoas.eeSubscription.EeSubscriptionModel
import io.tolgee.ee.api.v2.hateoas.eeSubscription.EeSubscriptionModelAssembler
import io.tolgee.ee.data.SetLicenseKeyDto
import io.tolgee.ee.service.EeSubscriptionService
import io.tolgee.security.AccessWithServerAdminPermission
import io.tolgee.service.security.SecurityService
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
  private val eeSubscriptionModelAssembler: EeSubscriptionModelAssembler
) {
  @PutMapping("set-license-key")
  @Operation(summary = "Sets the EE licence key for this instance")
  @AccessWithServerAdminPermission
  fun setLicenseKey(@RequestBody body: SetLicenseKeyDto): EeSubscriptionModel {
    val eeSubscription = eeSubscriptionService.setLicenceKey(body.licenseKey)
    return eeSubscriptionModelAssembler.toModel(eeSubscription)
  }

  @PostMapping("prepare-set-license-key")
  @Operation(summary = "Returns info about the upcoming EE subscription")
  @AccessWithServerAdminPermission
  fun prepareSetLicenseKey(@RequestBody body: SetLicenseKeyDto): PrepareSetEeLicenceKeyModel {
    return eeSubscriptionService.prepareSetLicenceKey(body.licenseKey)
  }

  @PutMapping("/refresh")
  @AccessWithServerAdminPermission
  fun refreshSubscription(): EeSubscriptionModel? {
    eeSubscriptionService.refreshSubscription()
    val eeSubscription = eeSubscriptionService.findSubscriptionEntity() ?: return null
    return eeSubscriptionModelAssembler.toModel(eeSubscription)
  }

  @GetMapping("info")
  @Operation(summary = "Returns the info about the current EE subscription")
  @AccessWithServerAdminPermission
  fun getInfo(): EeSubscriptionModel? {
    val eeSubscription = eeSubscriptionService.findSubscriptionEntity()
    return eeSubscription?.let { eeSubscriptionModelAssembler.toModel(it) }
  }

  @PutMapping("release-license-key")
  @Operation(summary = "Removes the EE licence key from this instance")
  @AccessWithServerAdminPermission
  fun release() {
    eeSubscriptionService.releaseSubscription()
  }
}
