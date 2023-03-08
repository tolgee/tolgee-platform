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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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
  @PutMapping(value = ["/{dateTimeString}"])
  @Operation(description = "Set's the time machine, so the app is using this date as current date")
  fun setTime(
    @PathVariable
    @Schema(description = "Current unix timestamp (milliseconds), or in yyyy-MM-dd HH:mm:ss z")
    dateTimeString: String
  ) {
    val timestamp = try {
      dateTimeString.toLong()
    } catch (e: Exception) {
      val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
      ZonedDateTime.parse(dateTimeString, formatter).toInstant().toEpochMilli()
    }
    currentDateProvider.forcedDate = Date(timestamp)
  }

  @DeleteMapping(value = [""])
  @Operation(description = "Releases the time machine, so the time is the current time")
  fun release() {
    currentDateProvider.forcedDate = null
  }
}
