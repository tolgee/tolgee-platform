package io.tolgee.controllers.internal

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.component.CurrentDateProvider
import io.tolgee.security.InternalController
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/time"])
@Transactional
@InternalController
class TimeMachineController(
  val currentDateProvider: CurrentDateProvider
) {
  @PutMapping(value = ["/{time}"])
  @Operation(description = "Set's the time machine, so the app is using this date as current date")
  fun setTime(@PathVariable @Schema(description = "Current unix timestamp (milliseconds)") time: Long) {
    currentDateProvider.forcedDate = Date(time)
  }

  @DeleteMapping(value = [""])
  @Operation(description = "Releases the time machine, so the time is the current time")
  fun release() {
    currentDateProvider.forcedDate = null
  }
}
