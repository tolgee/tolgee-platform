package io.tolgee.dtos.request.community

import io.swagger.v3.oas.annotations.Parameter
import io.tolgee.dtos.community.Granularity

class PeriodParams : RangeParams() {
  @field:Parameter(description = "One bucket per day (`DAY`) or one per week starting on Monday (`WEEK`).")
  var granularity: Granularity? = null
}
