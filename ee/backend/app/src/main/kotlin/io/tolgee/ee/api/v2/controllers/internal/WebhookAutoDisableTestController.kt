package io.tolgee.controllers.internal

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.component.automations.processors.WebhookAutoDisableChecker
import io.tolgee.ee.service.WebhookConfigService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/webhook-auto-disable"])
@ConditionalOnProperty(
  value = ["tolgee.internal.controller-enabled"],
  havingValue = "true",
  matchIfMissing = false,
)
class WebhookAutoDisableTestController(
  private val webhookAutoDisableChecker: WebhookAutoDisableChecker,
  private val webhookConfigService: WebhookConfigService,
) {
  @PostMapping("/check/{webhookId}")
  fun check(
    @PathVariable webhookId: Long,
  ) {
    val config = webhookConfigService.get(webhookId)
    webhookAutoDisableChecker.checkAfterFailure(config)
  }
}
