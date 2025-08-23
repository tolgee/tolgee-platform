package io.tolgee.controllers.internal

import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping

@InternalController(["internal/webhook-testing"])
class WebhookTestingController(
  val tolgeeProperties: TolgeeProperties,
) {
  @PostMapping(value = [""])
  @Transactional
  fun test(): ResponseEntity<String> {
    val statusCode = tolgeeProperties.internal.webhookControllerStatus
    return ResponseEntity("", HttpStatus.valueOf(statusCode))
  }
}
