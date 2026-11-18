package io.tolgee.dtos.request.community

import io.swagger.v3.oas.annotations.Parameter
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

open class RangeParams {
  @field:Parameter(description = "First day of the range, as an ISO date, included. $RANGE_LIMITS")
  @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  var from: LocalDate? = null

  @field:Parameter(
    description =
      "Last day of the range, as an ISO date, included. Defaults to today. " +
        "A `to` before 1970-01-01 or more than a year after today is moved to that bound.",
  )
  @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  var to: LocalDate? = null

  companion object {
    const val RANGE_LIMITS =
      "The range may cover at most 2 years; an earlier `from` is adjusted to 2 years before `to`. " +
        "If `from` is after `to`, the range is just the day `to`."
  }
}
