package io.tolgee.controllers.internal

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.component.CurrentDateProvider
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import java.util.Date

@InternalController(["internal/time"])
class TestClockController(
  val currentDateProvider: CurrentDateProvider,
) {
  @PutMapping(value = ["/{dateTimeString}"])
  @Operation(description = "Set's the time machine, so the app is using this date as current date")
  fun setTime(
    @PathVariable
    @Schema(description = "Current unix timestamp (milliseconds), or in yyyy-MM-dd HH:mm:ss z")
    dateTimeString: String,
  ) {
    try {
      currentDateProvider.forcedDate = Date(dateTimeString.toLong())
    } catch (e: NumberFormatException) {
      currentDateProvider.forceDateString(dateTimeString)
    }
  }

  @DeleteMapping(value = [""])
  @Operation(description = "Releases the time machine, so the time is the current time")
  fun release() {
    currentDateProvider.forcedDate = null
  }
}
