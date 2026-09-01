package io.tolgee.events

import org.springframework.context.ApplicationEvent

class OnBeforeOrganizationSeatIncrease(
  source: Any,
  val organizationId: Long,
) : ApplicationEvent(source)
