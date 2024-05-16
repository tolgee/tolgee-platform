package io.tolgee.events.user

import io.tolgee.model.UserAccount
import org.springframework.context.ApplicationEvent

abstract class UserAccountEvent(
  source: Any,
  val userAccount: UserAccount,
  val userSource: String? = null,
) : ApplicationEvent(source)
