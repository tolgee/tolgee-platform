package io.tolgee.dtos.request.community

import io.swagger.v3.oas.annotations.Parameter
import io.tolgee.dtos.community.Granularity
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

open class PeriodParams {
  @field:Parameter(description = "First day of the range, as an ISO date, included. $RANGE_LIMITS")
  @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  var from: LocalDate? = null

  @field:Parameter(description = "Last day of the range, as an ISO date, included. Defaults to today.")
  @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  var to: LocalDate? = null

  @field:Parameter(description = "One bucket per day (`DAY`) or one per week starting on Monday (`WEEK`).")
  var granularity: Granularity? = null

  companion object {
    const val RANGE_LIMITS =
      "The range may cover at most 2 years; an earlier `from` is adjusted to 2 years before `to`. " +
        "If `from` is after `to`, the range is just the day `to`."
  }
}
