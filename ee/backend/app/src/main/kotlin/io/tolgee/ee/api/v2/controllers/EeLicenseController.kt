package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.ee.api.v2.hateoas.assemblers.EeSubscriptionModelAssembler
import io.tolgee.ee.data.SetLicenseKeyDto
import io.tolgee.ee.service.eeSubscription.EeSubscriptionServiceImpl
import io.tolgee.hateoas.ee.PrepareSetEeLicenceKeyModel
import io.tolgee.hateoas.ee.eeSubscription.EeSubscriptionModel
import io.tolgee.openApiDocs.OpenApiEeExtension
import io.tolgee.security.authentication.RequiresSuperAuthentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v2/ee-license/")
@Tag(name = "EE Licence")
@OpenApiEeExtension
class EeLicenseController(
  private val eeSubscriptionService: EeSubscriptionServiceImpl,
  private val eeSubscriptionModelAssembler: EeSubscriptionModelAssembler,
) {
  @PutMapping("set-license-key")
  @Operation(summary = "Sets the EE licence key")
  @RequiresSuperAuthentication
  fun setLicenseKey(
    @RequestBody body: SetLicenseKeyDto,
  ): EeSubscriptionModel {
    val eeSubscription = eeSubscriptionService.setLicenceKey(body.licenseKey)
    return eeSubscriptionModelAssembler.toModel(eeSubscription.toDto())
  }

  @PostMapping("prepare-set-license-key")
  @Operation(
    summary = "Get info before applying the license key",
    description =
      "Get info about the upcoming EE subscription. This will show, how much " +
        "the subscription will cost when key is applied.",
  )
  @RequiresSuperAuthentication
  fun prepareSetLicenseKey(
    @RequestBody body: SetLicenseKeyDto,
  ): PrepareSetEeLicenceKeyModel {
    return eeSubscriptionService.prepareSetLicenceKey(body.licenseKey)
  }

  @PutMapping("/refresh")
  @RequiresSuperAuthentication
  @Operation(
    summary = "Refresh the EE subscription",
    description =
      "This will refresh the subscription information from the " +
        "license server and update the subscription info.",
  )
  fun refreshSubscription(): EeSubscriptionModel? {
    eeSubscriptionService.refreshSubscription()
    val eeSubscription = eeSubscriptionService.findSubscriptionEntity() ?: return null
    return eeSubscriptionModelAssembler.toModel(eeSubscription.toDto())
  }

  @GetMapping("info")
  @Operation(summary = "Get the info about the current EE subscription")
  @RequiresSuperAuthentication
  fun getInfo(): EeSubscriptionModel? {
    val eeSubscription = eeSubscriptionService.findSubscriptionEntity()
    return eeSubscription?.let { eeSubscriptionModelAssembler.toModel(it.toDto()) }
  }

  @PutMapping("release-license-key")
  @Operation(
    summary = "Remove the EE licence key",
    description = "This will remove the licence key from the instance.",
  )
  @RequiresSuperAuthentication
  fun release() {
    eeSubscriptionService.releaseSubscription()
  }
}
