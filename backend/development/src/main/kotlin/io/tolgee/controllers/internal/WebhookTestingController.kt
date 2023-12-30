package io.tolgee.controllers.internal

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.configuration.tolgee.TolgeeProperties
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/webhook-testing"])
@Transactional
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
