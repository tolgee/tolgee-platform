package io.tolgee.events.user

import io.tolgee.dtos.cacheable.UserAccountDto
import org.springframework.context.ApplicationEvent

class OnUserUpdated(
  source: Any,
  val oldUserAccount: UserAccountDto,
  val newUserAccount: UserAccountDto,
) : ApplicationEvent(source)
