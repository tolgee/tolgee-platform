package io.tolgee.dtos.request

import javax.validation.constraints.NotBlank

data class BusinessEventReportRequest(
  @field:NotBlank
  var eventName: String = "",

  var anonymousUserId: String? = null,

  var organizationId: Long? = null,

  var projectId: Long? = null,

  val data: Map<String, Any?>? = null
)
