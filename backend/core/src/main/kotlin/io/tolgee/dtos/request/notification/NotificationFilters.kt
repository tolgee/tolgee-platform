package io.tolgee.dtos.request.notification

import io.swagger.v3.oas.annotations.Parameter

data class NotificationFilters(
  @field:Parameter(
    description = """Filter by the `seen` parameter.

no value = request everything

true = only seen

false = only unseen""",
  )
  val filterSeen: Boolean? = null,
)
