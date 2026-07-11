package io.tolgee.ee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.ee.service.eeSubscription.EeSubscriptionUsageService
import io.tolgee.hateoas.ee.uasge.current.CurrentUsageModel
import io.tolgee.openApiDocs.OpenApiEeExtension
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 *
 * It returns info about the current ee subscription on self-hosted instance.
 *
 * This controller is relevant only for self-hosted instances.
 * It doesn't make sense on Tolgee Cloud.
 */
@RestController
@RequestMapping("/v2/ee-current-subscription-usage")
@Tag(name = "EE Licence")
@OpenApiEeExtension
class EeSubscriptionUsageController(
  private val usageService: EeSubscriptionUsageService,
) {
  @GetMapping("")
  @Operation(summary = "Get current usage for the current EE subscription")
  fun getUsage(): CurrentUsageModel? {
    return usageService.getUsage()
  }
}
