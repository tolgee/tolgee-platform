package io.tolgee.controllers.internal

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.component.automations.processors.WebhookAutoDisableChecker
import io.tolgee.repository.WebhookConfigRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.transaction.annotation.Transactional
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
  private val webhookAutoDisableChecker: WebhookAutoDisableChecker,
  private val webhookConfigRepository: WebhookConfigRepository,
) {
  @PostMapping("/check-all")
  @Transactional
  fun checkAll() {
    webhookConfigRepository.findAll().forEach { config ->
      webhookAutoDisableChecker.checkAfterFailure(config)
    }
  }
}
