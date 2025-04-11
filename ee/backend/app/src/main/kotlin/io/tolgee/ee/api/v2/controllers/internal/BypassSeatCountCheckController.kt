package io.tolgee.controllers.internal

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.ee.service.eeSubscription.EeSubscriptionServiceImpl
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/bypass-seat-count-check"])
@Transactional
class BypassSeatCountCheckController(
  val tolgeeProperties: TolgeeProperties,
  val eeSubscriptionServiceImpl: EeSubscriptionServiceImpl?,
) {
  @PutMapping(value = ["/set"])
  @Transactional
  fun setProperty(
    @RequestParam value: Boolean,
  ) {
    eeSubscriptionServiceImpl?.let { it.bypassSeatCountCheck = value }
  }
}
