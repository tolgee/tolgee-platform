package io.tolgee.component.reporting

import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.cacheable.UserAccountDto

data class OnBusinessEventToCaptureEvent(
  val eventName: String,
  val instanceId: String? = null,
  val projectDto: ProjectDto? = null,
  val projectId: Long? = null,
  val organizationId: Long? = null,
  val organizationName: String? = null,
  val userAccountId: Long? = null,
  val userAccountDto: UserAccountDto? = null,
  val utmData: Map<String, Any?>? = null,
  val sdkType: String? = null,
  val sdkVersion: String? = null,
  val data: Map<String, Any?>? = null,
  val anonymousUserId: String? = null,
)
