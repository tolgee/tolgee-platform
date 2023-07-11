package io.tolgee.component.reporting

import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.cacheable.UserAccountDto

data class OnEventToCaptureEvent(
  val eventName: String,
  val projectDto: ProjectDto? = null,
  val projectId: Long? = null,
  val organizationId: Long? = null,
  val organizationName: String? = null,
  val userAccountId: Long? = null,
  val userAccountDto: UserAccountDto? = null,
  val utmData: Map<String, Any?>? = null
)
