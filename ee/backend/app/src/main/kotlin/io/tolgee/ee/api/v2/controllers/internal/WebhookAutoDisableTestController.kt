package io.tolgee.controllers.internal

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.ee.component.WebhookAutoDisableScheduler
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.CrossOrigin
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
  private val webhookAutoDisableScheduler: WebhookAutoDisableScheduler,
) {
  @PostMapping("/run")
  fun run() {
    webhookAutoDisableScheduler.checkAndDisable()
  }
}
