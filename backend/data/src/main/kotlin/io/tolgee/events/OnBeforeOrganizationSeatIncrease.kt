package io.tolgee.events

import org.springframework.context.ApplicationEvent

/** Publish only once the increase is known to be otherwise permitted, and before it is committed. */
class OnBeforeOrganizationSeatIncrease(
  source: Any,
  val organizationId: Long,
) : ApplicationEvent(source)
